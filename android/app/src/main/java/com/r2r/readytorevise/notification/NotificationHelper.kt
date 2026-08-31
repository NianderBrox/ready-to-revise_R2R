package com.r2r.readytorevise.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import com.r2r.readytorevise.MainActivity
import com.r2r.readytorevise.R

object NotificationHelper {

    const val CHANNEL_ID = "revision_reminders"
    const val CHANNEL_NAME = "Revision Reminders"
    const val NOTIFICATION_ID_REVISION_DUE = 1001
    const val NOTIFICATION_ID_NO_REVISION = 1002

    const val ACTION_SNOOZE_1H = "com.r2r.readytorevise.SNOOZE_1H"
    const val ACTION_SNOOZE_2H = "com.r2r.readytorevise.SNOOZE_2H"
    const val ACTION_SNOOZE_4H = "com.r2r.readytorevise.SNOOZE_4H"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for daily revision reminders"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun sendRevisionDueNotification(context: Context, itemCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze1hIntent = createSnoozeIntent(context, ACTION_SNOOZE_1H, NOTIFICATION_ID_REVISION_DUE)
        val snooze2hIntent = createSnoozeIntent(context, ACTION_SNOOZE_2H, NOTIFICATION_ID_REVISION_DUE)
        val snooze4hIntent = createSnoozeIntent(context, ACTION_SNOOZE_4H, NOTIFICATION_ID_REVISION_DUE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(createSmallAppIcon(context))
            .setContentTitle("You have revisions today!")
            .setContentText("$itemCount items are due for revision. Start now to keep your streak going!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Remind in 1 hour", snooze1hIntent)
            .addAction(0, "Remind in 2 hours", snooze2hIntent)
            .addAction(0, "Remind in 4 hours", snooze4hIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_REVISION_DUE, notification)
    }

    fun sendNoRevisionNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(createSmallAppIcon(context))
            .setContentTitle("No revisions today")
            .setContentText("Great news! You have no pending revisions. Enjoy your free day!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_NO_REVISION, notification)
    }

    private fun createSmallAppIcon(context: Context): Bitmap {
        val iconSize = 48
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
            ?: context.applicationContext.packageManager.getApplicationIcon(context.applicationContext.packageName)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createSnoozeIntent(context: Context, action: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            this.action = action
            putExtra("notification_id", notificationId)
        }
        return PendingIntent.getBroadcast(
            context, notificationId + action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
