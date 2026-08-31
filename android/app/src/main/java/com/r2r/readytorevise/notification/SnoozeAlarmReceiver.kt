package com.r2r.readytorevise.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SnoozeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(
            SnoozeReceiver.EXTRA_NOTIFICATION_ID,
            NotificationHelper.NOTIFICATION_ID_REVISION_DUE
        )

        when (notificationId) {
            NotificationHelper.NOTIFICATION_ID_REVISION_DUE -> {
                NotificationHelper.sendRevisionDueNotification(context, 0)
            }
        }
    }
}
