package com.r2r.readytorevise.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.TimeUnit

class SnoozeReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val SNOOZE_REQUEST_CODE_BASE = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.NOTIFICATION_ID_REVISION_DUE)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notificationId)

        val delayMillis = when (intent.action) {
            NotificationHelper.ACTION_SNOOZE_1H -> TimeUnit.HOURS.toMillis(1)
            NotificationHelper.ACTION_SNOOZE_2H -> TimeUnit.HOURS.toMillis(2)
            NotificationHelper.ACTION_SNOOZE_4H -> TimeUnit.HOURS.toMillis(4)
            else -> return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(context, SnoozeAlarmReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE_BASE + notificationId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + delayMillis
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
