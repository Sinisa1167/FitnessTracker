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
            .setContentText("Nisi bio aktivan duže od 48 sati. Hajde na trening!")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    companion object {
        private const val WORK_NAME      = "reminder_work"
        private const val DELAY_HOURS    = 48L

        fun scheduleFromNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(DELAY_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun scheduleWithDelay(context: Context, delayMillis: Long) {
            if (delayMillis <= 0L) {
                scheduleFromNow(context)
                return
            }
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}