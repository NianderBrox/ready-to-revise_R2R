package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.r2r.readytorevise.di.DefaultAppContainer
import com.r2r.readytorevise.notification.UploadNotifications
import kotlinx.coroutines.delay

class AnalysisPollWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val batchId = inputData.getString(UploadWork.IN_BATCH_ID)
            ?: return fail("Missing batch", emptyList())
        val docIds = inputData.getStringArray(UploadWork.IN_DOC_IDS) ?: emptyArray()
        val names = inputData.getStringArray(UploadWork.IN_NAMES) ?: emptyArray()
        val total = inputData.getInt(UploadWork.IN_TOTAL, docIds.size)
        if (docIds.isEmpty()) {
            return fail("Nothing to process", emptyList(), batchId)
        }
        setForeground(
            UploadNotifications.foregroundInfo(
                applicationContext, batchId,
                UploadWork.STAGE_ANALYZE, 0, total, null, "Analyzing…",
            ),
        )
        val repository = DefaultAppContainer(applicationContext).documentsRepository
        val pending = docIds.toMutableList()
        repeat(POLL_ROUNDS) {
            val iterator = pending.iterator()
            while (iterator.hasNext()) {
                val id = iterator.next()
                val current = repository.getDocument(id).getOrNull()
                when (current?.status) {
                    UploadWork.STATUS_READY -> iterator.remove()
                    UploadWork.STATUS_FAILED -> return fail("Processing failed", names.toList(), batchId)
                }
            }
            val ready = docIds.size - pending.size
            val progress = workDataOf(
                UploadWork.P_STAGE to UploadWork.STAGE_ANALYZE,
                UploadWork.P_COMPLETED to ready,
                UploadWork.P_TOTAL to total,
                UploadWork.P_MESSAGE to "Analyzing… ($ready of ${docIds.size} ready)",
            )
            setProgress(progress)
            UploadNotifications.showProgress(
                applicationContext, batchId,
                ready, total, null, "Analyzing… ($ready of ${docIds.size} ready)",
            )
            if (pending.isEmpty()) {
                return Result.success(UploadWork.forward(inputData))
            }
            delay(POLL_INTERVAL_MS)
        }
        saveDescriptor(batchId, docIds.toList())
        UploadNotifications.showError(
            applicationContext, batchId,
            "Still processing — retry to check again", false,
        )
        return Result.failure(
            UploadWork.forward(
                inputData,
                UploadWork.OUT_ERROR to "Still processing — retry to check again",
                UploadWork.OUT_FAILED_FILES to names,
            ),
        )
    }

    private suspend fun saveDescriptor(batchId: String, docIds: List<String>) {
        UploadPrefs(applicationContext).saveFailed(
            FailedBatch(
                batchId = batchId,
                names = inputData.getStringArray(UploadWork.IN_NAMES)?.toList().orEmpty(),
                mimes = inputData.getStringArray(UploadWork.IN_MIMES)?.toList().orEmpty(),
                docIds = docIds,
            ),
        )
    }

    private suspend fun fail(message: String, failedFiles: List<String>, batchId: String? = null): Result {
        if (batchId != null) {
            saveDescriptor(batchId, inputData.getStringArray(UploadWork.IN_DOC_IDS)?.toList().orEmpty())
            UploadNotifications.showError(applicationContext, batchId, message, false)
        }
        return Result.failure(
            UploadWork.forward(
                inputData,
                UploadWork.OUT_ERROR to message,
                UploadWork.OUT_FAILED_FILES to failedFiles.toTypedArray(),
            ),
        )
    }

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
        const val POLL_ROUNDS = 60
    }
}
