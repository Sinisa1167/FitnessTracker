package com.example.fitnesstracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.osmdroid.config.Configuration

class FitnessApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val trackingChannel = NotificationChannel(
            CHANNEL_TRACKING,
            getString(R.string.notif_channel_tracking),
            NotificationManager.IMPORTANCE_LOW
        )

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER,
            getString(R.string.notif_channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        manager.createNotificationChannel(trackingChannel)
        manager.createNotificationChannel(reminderChannel)
    }

    companion object {
        const val CHANNEL_TRACKING = "channel_tracking"
        const val CHANNEL_REMINDER = "channel_reminder"
    }
}