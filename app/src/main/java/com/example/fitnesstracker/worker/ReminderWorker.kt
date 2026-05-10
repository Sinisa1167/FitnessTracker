package com.example.fitnesstracker.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.fitnesstracker.FitnessApp
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        sendReminderNotification()
        return Result.success()
    }

    private fun sendReminderNotification() {
        val notification = NotificationCompat.Builder(applicationContext, FitnessApp.CHANNEL_REMINDER)
            .setContentTitle("Vrijeme za aktivnost!")
            .setContentText("Nisi bio aktivan duže vrijeme. Hajde na trening!")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    companion object {
        private const val WORK_NAME = "reminder_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(2, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}