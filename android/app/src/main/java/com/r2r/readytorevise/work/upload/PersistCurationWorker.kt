package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.di.DefaultAppContainer
import com.r2r.readytorevise.notification.UploadNotifications
import java.io.File
import kotlinx.serialization.json.Json

class PersistCurationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val batchId = inputData.getString(UploadWork.IN_BATCH_ID) ?: return Result.success(
            workDataOf(UploadWork.OUT_KEPT to 0),
        )
        val generatedFile = inputData.getString(UploadWork.IN_GENERATED_FILE)
        val discarded = inputData.getStringArray(UploadWork.IN_DISCARD_IDS)?.toSet().orEmpty()
        setForeground(
            UploadNotifications.foregroundInfo(
                applicationContext, batchId,
                UploadWork.STAGE_SAVE, 0, 1, null, "Saving…",
            ),
        )
        val repository = DefaultAppContainer(applicationContext).studyItemsRepository
        var failures = 0
        for (id in discarded) {
            repository.deleteStudyItem(id).onFailure { failures += 1 }
        }
        var kept = 0
        if (generatedFile != null) {
            try {
                val total = json.decodeFromString<List<StudyItemDto>>(File(generatedFile).readText()).size
                kept = total - discarded.size + failures
            } catch (t: Throwable) {
                kept = 0
            }
            File(generatedFile).delete()
        }
        File(applicationContext.cacheDir, UploadWork.batchDirName(batchId)).deleteRecursively()
        UploadPrefs(applicationContext).setActiveBatchId(null)
        UploadPrefs(applicationContext).clearFailed()
        UploadNotifications.showDone(applicationContext, kept, failures)
        return Result.success(workDataOf(UploadWork.OUT_KEPT to kept))
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
