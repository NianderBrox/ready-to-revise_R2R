package com.r2r.readytorevise.work.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.r2r.readytorevise.notification.UploadNotifications
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UploadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val batchId = intent.getStringExtra(UploadWork.EXTRA_BATCH_ID)
                when (intent.action) {
                    UploadWork.ACTION_CANCEL_BATCH -> {
                        if (batchId != null) {
                            WorkManager.getInstance(app).cancelUniqueWork(UploadWork.batchName(batchId))
                            File(app.cacheDir, UploadWork.batchDirName(batchId)).deleteRecursively()
                            File(app.cacheDir, UploadWork.batchDirName(batchId) + "_generated.json").delete()
                            val prefs = UploadPrefs(app)
                            val active = try {
                                prefs.activeBatchId.first()
                            } catch (t: Throwable) {
                                null
                            }
                            if (active == batchId) {
                                prefs.setActiveBatchId(null)
                            }
                            UploadNotifications.dismiss(app)
                        }
                    }
                    UploadWork.ACTION_RETRY_BATCH -> {
                        UploadWorkScheduler.resumeFailed(app)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
