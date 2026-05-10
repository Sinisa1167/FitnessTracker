package com.example.fitnesstracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.fitnesstracker.FitnessApp
import com.example.fitnesstracker.MainActivity
import com.google.android.gms.location.*

class TrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        val isTracking = MutableLiveData(false)
        val pathPoints = MutableLiveData<MutableList<Location>>(mutableListOf())
        val distanceMeters = MutableLiveData(0f)
        val elapsedSeconds = MutableLiveData(0L)

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private var startTimeMillis = 0L
    private var timerRunning = false
    private val timerThread = object : Thread() {
        override fun run() {
            while (timerRunning) {
                elapsedSeconds.postValue((System.currentTimeMillis() - startTimeMillis) / 1000)
                sleep(1000)
            }
        }
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
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startTracking() {
        isTracking.postValue(true)
        pathPoints.postValue(mutableListOf())
        distanceMeters.postValue(0f)
        startTimeMillis = System.currentTimeMillis()
        timerRunning = true
        timerThread.start()
        startForeground(1, buildNotification())
        requestLocationUpdates()
    }

    private fun stopTracking() {
        isTracking.postValue(false)
        timerRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        points.add(location)
        pathPoints.postValue(points)
    }

    @SuppressWarnings("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FitnessApp.CHANNEL_TRACKING)
            .setContentTitle("Praćenje aktivnosti")
            .setContentText("Aktivnost je u toku...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}