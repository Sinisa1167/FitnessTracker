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
import com.example.fitnesstracker.util.DateUtils

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val result = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesManager(context)
                val notificationsOn = prefs.notificationsEnabled.first()
                if (!notificationsOn) return@launch

                val lastActivity = prefs.getLastActivityTimestamp()
                if (lastActivity == 0L) {
                    ReminderWorker.scheduleFromNow(context)
                    return@launch
                }

                val startOfToday = DateUtils.startOfTodayMillis()

                if (lastActivity >= startOfToday) return@launch

                val elapsed = System.currentTimeMillis() - lastActivity
                val delayMs = TimeUnit.HOURS.toMillis(48) - elapsed

                ReminderWorker.scheduleWithDelay(context, delayMs)
            } finally {
                result.finish()
            }
        }
    }
}