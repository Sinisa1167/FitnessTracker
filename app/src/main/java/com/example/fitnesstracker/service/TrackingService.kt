package com.example.fitnesstracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.HandlerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.fitnesstracker.FitnessApp
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.model.ActivityType
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

private const val MAX_ACCEPTABLE_ACCURACY_METERS = 20f
private const val MAX_ACCEPTABLE_SPEED_ACCURACY_MPS = 1.5f
private const val STATIONARY_DISTANCE_THRESHOLD_METERS = 2f
private const val SPEED_SMOOTHING_SAMPLE_COUNT = 5
private const val MIN_SPEED_KMH_THRESHOLD = 1f

class TrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationHandlerThread: HandlerThread
    private var startTimeMillis = 0L
    private var movingTimeMillis = 0L
    private var lastResumeTimeMillis = 0L
    private var recentSpeedSamples = ArrayDeque<Float>()
    private var lastDistancePoint: Location? = null
    private var timerJob: Job? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activityTypeKey: String = ActivityType.RUNNING.key

    companion object {
        val isTracking      = MutableLiveData(false)
        val isPaused        = MutableLiveData(false)
        val pathPoints      = MutableLiveData<MutableList<Location>>(mutableListOf())
        val distanceMeters  = MutableLiveData(0f)
        val elapsedSeconds  = MutableLiveData(0L)
        val currentSpeedKmh = MutableLiveData(0f)
        val avgSpeedKmh     = MutableLiveData(0f)

        const val ACTION_START      = "ACTION_START"
        const val ACTION_STOP       = "ACTION_STOP"
        const val ACTION_PAUSE      = "ACTION_PAUSE"
        const val ACTION_RESUME     = "ACTION_RESUME"
        const val EXTRA_NAVIGATE_TO   = "navigate_to"
        const val EXTRA_ACTIVITY_TYPE = "activity_type"

        fun maxPlausibleSpeedKmh(typeKey: String): Float = when (ActivityType.fromKey(typeKey)) {
            ActivityType.WALKING, ActivityType.HIKING -> 10f
            ActivityType.RUNNING  -> 25f
            ActivityType.SWIMMING -> 8f
            ActivityType.CYCLING  -> 50f
            ActivityType.OTHER    -> 40f
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationHandlerThread = HandlerThread("LocationThread").also { it.start() }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { addPathPoint(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                intent.getStringExtra(EXTRA_ACTIVITY_TYPE)?.let { activityTypeKey = it }
                startTracking()
            }
            ACTION_STOP   -> stopTracking()
            ACTION_PAUSE  -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startTracking() {
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasLocationPermission())
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            else
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        ServiceCompat.startForeground(this, 1, buildNotification(), serviceType)

        isTracking.postValue(true)
        isPaused.postValue(false)
        recentSpeedSamples.clear()
        lastDistancePoint = null
        pathPoints.postValue(mutableListOf())
        distanceMeters.postValue(0f)
        elapsedSeconds.postValue(0L)
        currentSpeedKmh.postValue(0f)
        avgSpeedKmh.postValue(0f)

        startTimeMillis = System.currentTimeMillis()
        movingTimeMillis = 0L
        lastResumeTimeMillis = startTimeMillis
        startTimer()
        requestLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                elapsedSeconds.postValue((System.currentTimeMillis() - startTimeMillis) / 1000)
                delay(1000)
            }
        }
    }

    private fun pauseTracking() {
        isPaused.postValue(true)
        currentSpeedKmh.postValue(0f)
        movingTimeMillis += System.currentTimeMillis() - lastResumeTimeMillis
        timerJob?.cancel()
        try { fusedLocationClient.removeLocationUpdates(locationCallback) } catch (e: Exception) {}
    }

    private fun resumeTracking() {
        isPaused.postValue(false)
        val pausedSeconds = elapsedSeconds.value ?: 0L
        startTimeMillis = System.currentTimeMillis() - (pausedSeconds * 1000L)
        lastResumeTimeMillis = System.currentTimeMillis()
        startTimer()
        requestLocationUpdates()
    }

    private fun stopTracking() {
        timerJob?.cancel()
        serviceScope.cancel()
        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        isTracking.postValue(false)
        isPaused.postValue(false)
        currentSpeedKmh.postValue(0f)
        try { fusedLocationClient.removeLocationUpdates(locationCallback) } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        locationHandlerThread.quitSafely()
    }

    private fun addPathPoint(location: Location) {
        if (location.hasAccuracy() && location.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) return

        val points = (pathPoints.value ?: mutableListOf()).toMutableList()
        val maxSpeedKmh = maxPlausibleSpeedKmh(activityTypeKey)

        if (points.isNotEmpty()) {
            val last = points.last()
            val result = FloatArray(1)
            Location.distanceBetween(
                last.latitude, last.longitude,
                location.latitude, location.longitude,
                result
            )
            val rawDeltaFromLast = result[0]

            val timeDeltaSec = (location.time - last.time) / 1000f
            if (timeDeltaSec > 0f) {
                val impliedSpeedKmh = (rawDeltaFromLast / timeDeltaSec) * 3.6f
                if (impliedSpeedKmh > maxSpeedKmh) return // GPS glitch, odbaci cijelu tačku
            }

            // Umjesto mjerenja od svake prethodne tačke, mjerimo od zadnje PRIHVAĆENE
            // distance-referentne tačke. Mali pomaci ispod praga se ne gube - naprosto
            // se ne "pomjera" referenca, pa se sljedeći pomak nadodaje na već postojeći
            // dok ukupno ne pređe prag. Time se izbjegava gubitak stvarnog kretanja
            // usled GPS šuma kod sporih aktivnosti (npr. hodanje).
            val distanceRef = lastDistancePoint ?: last
            val refResult = FloatArray(1)
            Location.distanceBetween(
                distanceRef.latitude, distanceRef.longitude,
                location.latitude, location.longitude,
                refResult
            )
            val distanceFromRef = refResult[0]

            if (distanceFromRef >= STATIONARY_DISTANCE_THRESHOLD_METERS) {
                distanceMeters.postValue((distanceMeters.value ?: 0f) + distanceFromRef)
                lastDistancePoint = location
            }
        } else {
            lastDistancePoint = location
        }

        val rawSpeedKmh = if (location.hasSpeed() &&
            location.hasSpeedAccuracy() &&
            location.speedAccuracyMetersPerSecond < MAX_ACCEPTABLE_SPEED_ACCURACY_MPS &&
            location.speed * 3.6f <= maxSpeedKmh
        ) location.speed * 3.6f else 0f

        recentSpeedSamples.addLast(rawSpeedKmh)
        if (recentSpeedSamples.size > SPEED_SMOOTHING_SAMPLE_COUNT) recentSpeedSamples.removeFirst()
        val smoothedSpeedKmh = recentSpeedSamples.average().toFloat()
        val displaySpeedKmh = if (smoothedSpeedKmh < MIN_SPEED_KMH_THRESHOLD) 0f else smoothedSpeedKmh
        currentSpeedKmh.postValue(displaySpeedKmh)

        val totalDistanceKm = (distanceMeters.value ?: 0f) / 1000f
        val movingSeconds = (movingTimeMillis + (System.currentTimeMillis() - lastResumeTimeMillis)) / 1000f
        if (movingSeconds > 0f) {
            avgSpeedKmh.postValue(totalDistanceKm / (movingSeconds / 3600f))
        }

        points.add(location)
        pathPoints.postValue(points)
    }

    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) {
            android.util.Log.w("TrackingService", "Tracking started without GPS permissions.")
            return
        }

        val hasFine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY
        else         Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val intervalMillis = when (ActivityType.fromKey(activityTypeKey)) {
            ActivityType.CYCLING -> 1000L
            else -> 2000L
        }

        val request = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMaxUpdateDelayMillis(intervalMillis)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, locationHandlerThread.looper)
        } catch (e: SecurityException) {
            android.util.Log.e("TrackingService", "SecurityException: ${e.message}")
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_NAVIGATE_TO, "tracking")
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, FitnessApp.CHANNEL_TRACKING)
            .setContentTitle(getString(R.string.notif_tracking_title))
            .setContentText(getString(R.string.notif_tracking_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}