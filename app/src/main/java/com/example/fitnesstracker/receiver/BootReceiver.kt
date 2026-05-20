package com.example.fitnesstracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import com.example.fitnesstracker.data.PreferencesManager
import com.example.fitnesstracker.worker.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val prefs           = PreferencesManager(context)
            val notificationsOn = prefs.notificationsEnabled.first()
            if (!notificationsOn) return@launch

            val lastActivity = prefs.getLastActivityTimestamp()
            if (lastActivity == 0L) {
                ReminderWorker.scheduleFromNow(context)
                return@launch
            }

            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (lastActivity >= startOfToday) return@launch

            val elapsed  = System.currentTimeMillis() - lastActivity
            val delayMs  = TimeUnit.HOURS.toMillis(48) - elapsed

            ReminderWorker.scheduleWithDelay(context, delayMs)
        }
    }
}