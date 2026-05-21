package com.example.fitnesstracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.fitnesstracker.FitnessApp
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class TrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var startTimeMillis = 0L
    private val speedSamples = mutableListOf<Float>()
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val isTracking      = MutableLiveData(false)
        val isPaused        = MutableLiveData(false)
        val pathPoints      = MutableLiveData<MutableList<Location>>(mutableListOf())
        val distanceMeters  = MutableLiveData(0f)
        val elapsedSeconds  = MutableLiveData(0L)
        val currentSpeedKmh = MutableLiveData(0f)
        val avgSpeedKmh = MutableLiveData(0f)

        const val ACTION_START      = "ACTION_START"
        const val ACTION_STOP       = "ACTION_STOP"
        const val ACTION_PAUSE      = "ACTION_PAUSE"
        const val ACTION_RESUME     = "ACTION_RESUME"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { addPathPoint(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> startTracking()
            ACTION_STOP   -> stopTracking()
            ACTION_PAUSE  -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startTracking() {
        startForeground(1, buildNotification())
        isTracking.postValue(true)
        isPaused.postValue(false)
        pathPoints.postValue(mutableListOf())
        distanceMeters.postValue(0f)
        elapsedSeconds.postValue(0L)

        startTimeMillis = System.currentTimeMillis()
        startTimer()
        requestLocationUpdates()
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
        timerJob?.cancel()
        try { fusedLocationClient.removeLocationUpdates(locationCallback) } catch (e: Exception) {}
    }

    private fun resumeTracking() {
        isPaused.postValue(false)
        val pausedSeconds = elapsedSeconds.value ?: 0L
        startTimeMillis   = System.currentTimeMillis() - (pausedSeconds * 1000L)
        startTimer()
        requestLocationUpdates()
    }

    private fun stopTracking() {
        timerJob?.cancel()
        serviceScope.cancel()
        isTracking.postValue(false)
        isPaused.postValue(false)
        currentSpeedKmh.postValue(0f)
        try { fusedLocationClient.removeLocationUpdates(locationCallback) } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun addPathPoint(location: Location) {
        val points = pathPoints.value ?: mutableListOf()
        if (points.isNotEmpty()) {
            val last = points.last()
            val result = FloatArray(1)
            Location.distanceBetween(
                last.latitude, last.longitude,
                location.latitude, location.longitude,
                result
            )
            distanceMeters.postValue((distanceMeters.value ?: 0f) + result[0])
        }

        if (location.hasSpeed() && location.speed > 0.3f) {
            val kmh = location.speed * 3.6f
            speedSamples.add(kmh)
            currentSpeedKmh.postValue(kmh)
            avgSpeedKmh.postValue(speedSamples.average().toFloat())
        }

        points.add(location)
        pathPoints.postValue(points)
    }

    private fun requestLocationUpdates() {
        val hasFine   = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            android.util.Log.w("TrackingService", "Tracking started without GPS permissions.")
            return
        }

        val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY
        else         Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val request = LocationRequest.Builder(priority, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
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
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}