package com.example.fitnesstracker.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.fitnesstracker.FitnessApp
import java.util.concurrent.TimeUnit
import com.example.fitnesstracker.R

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        sendReminderNotification()
        return Result.success()
    }

    private fun sendReminderNotification() {
        val notification = NotificationCompat.Builder(applicationContext, FitnessApp.CHANNEL_REMINDER)
            .setContentTitle(applicationContext.getString(R.string.notif_reminder_title))
            .setContentText(applicationContext.getString(R.string.notif_reminder_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        applicationContext.getSystemService(NotificationManager::class.java).notify(2, notification)
    }

    companion object {
        private const val WORK_NAME = "reminder_work"

        fun scheduleFromNow(context: Context, hours: Int) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(hours.toLong(), TimeUnit.HOURS)
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