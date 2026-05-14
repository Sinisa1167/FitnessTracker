package com.example.fitnesstracker.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.fitnesstracker.FitnessApp
import com.example.fitnesstracker.data.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val hours = PreferencesManager(applicationContext).reminderHours.first()
        sendReminderNotification(hours)
        return Result.success()
    }

    private fun sendReminderNotification(hours: Int) {
        val notification = NotificationCompat.Builder(applicationContext, FitnessApp.CHANNEL_REMINDER)
            .setContentTitle("Vrijeme za aktivnost!")
            .setContentText("Nisi bio aktivan duže od ${hours}h. Hajde na trening!")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setAutoCancel(true)
            .build()

        applicationContext.getSystemService(NotificationManager::class.java).notify(2, notification)
    }

    companion object {
        private const val WORK_NAME = "reminder_work"

        suspend fun scheduleFromNow(context: Context) {
            val hours = PreferencesManager(context).reminderHours.first()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(hours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }

        fun scheduleWithDelay(context: Context, delayMillis: Long) {
            if (delayMillis <= 0L) return
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}