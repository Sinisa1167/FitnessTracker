package com.example.fitnesstracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitnesstracker.worker.ReminderWorker
import androidx.work.*
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderWorker.schedule(context)
        }
    }
}