package com.r2r.readytorevise.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.r2r.readytorevise.MainActivity
import com.r2r.readytorevise.R
import com.r2r.readytorevise.work.upload.UploadActionReceiver
import com.r2r.readytorevise.work.upload.UploadWork

object UploadNotifications {
    const val CHANNEL_ID = "upload_progress"
    const val CHANNEL_NAME = "Uploads"

    fun createUploadChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Background document upload progress"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun foregroundInfo(
        context: Context,
        batchId: String,
        stage: String,
        completed: Int,
        total: Int,
        percent: Int?,
        message: String,
    ): ForegroundInfo {
        val notification = builder(context, batchId, stageTitle(stage), message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                if (percent != null) {
                    setProgress(100, percent, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .addAction(0, "Cancel", cancelIntent(context, batchId))
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                UploadWork.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(UploadWork.NOTIFICATION_ID, notification)
        }
    }

    fun showProgress(
        context: Context,
        batchId: String,
        completed: Int,
        total: Int,
        percent: Int?,
        message: String,
    ) {
        val notification = builder(context, batchId, "Uploading", message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                if (percent != null) {
                    setProgress(100, percent, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .addAction(0, "Cancel", cancelIntent(context, batchId))
            .build()
        notify(context, notification)
    }

    fun showAwaitingSelection(context: Context, batchId: String, count: Int) {
        val notification = builder(
            context, batchId,
            "Questions ready",
            "$count questions generated — tap to review",
        )
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        notify(context, notification)
    }

    fun showDone(context: Context, kept: Int, failures: Int) {
        val text = "$kept question${if (kept == 1) "" else "s"} saved" +
            if (failures > 0) " · $failures removals failed" else ""
        val notification = builder(context, "", "Upload complete", text)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        notify(context, notification)
    }

    fun showError(context: Context, batchId: String, message: String, loginRequired: Boolean) {
        val builder = builder(context, batchId, "Upload failed", message)
            .setOngoing(false)
            .setAutoCancel(false)
        if (loginRequired) {
            builder.addAction(0, "Open app", contentIntent(context))
        } else {
            builder.addAction(0, "Retry", retryIntent(context, batchId))
            builder.addAction(0, "Open app", contentIntent(context))
        }
        notify(context, builder.build())
    }

    fun dismiss(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(UploadWork.NOTIFICATION_ID)
    }

    private fun builder(context: Context, batchId: String, title: String, message: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent(context))
    }

    private fun stageTitle(stage: String): String {
        return when (stage) {
            UploadWork.STAGE_PREPARE -> "Preparing"
            UploadWork.STAGE_UPLOAD -> "Uploading"
            UploadWork.STAGE_ANALYZE -> "Analyzing"
            UploadWork.STAGE_GENERATE -> "Generating"
            UploadWork.STAGE_SAVE -> "Saving"
            else -> "Working"
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(UploadWork.EXTRA_OPEN_PROCESSING, true)
        }
        return PendingIntent.getActivity(
            context, UploadWork.EXTRA_OPEN_PROCESSING.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelIntent(context: Context, batchId: String): PendingIntent {
        val intent = Intent(context, UploadActionReceiver::class.java).apply {
            action = UploadWork.ACTION_CANCEL_BATCH
            putExtra(UploadWork.EXTRA_BATCH_ID, batchId)
        }
        return PendingIntent.getBroadcast(
            context, batchId.hashCode() + 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun retryIntent(context: Context, batchId: String): PendingIntent {
        val intent = Intent(context, UploadActionReceiver::class.java).apply {
            action = UploadWork.ACTION_RETRY_BATCH
            putExtra(UploadWork.EXTRA_BATCH_ID, batchId)
        }
        return PendingIntent.getBroadcast(
            context, batchId.hashCode() + 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(context: Context, notification: android.app.Notification) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(UploadWork.NOTIFICATION_ID, notification)
    }
}
