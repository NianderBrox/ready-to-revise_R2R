package com.r2r.readytorevise.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = NotificationPreferences(context)
        val enabled = runBlocking { prefs.notificationsEnabled.first() }
        if (!enabled) return

        val hour = runBlocking { prefs.notificationHour.first() }
        val minute = runBlocking { prefs.notificationMinute.first() }

        scheduleDailyReminder(context, hour, minute)
    }

    companion object {
        fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }

            val delayMillis = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<RevisionReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "revision_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
