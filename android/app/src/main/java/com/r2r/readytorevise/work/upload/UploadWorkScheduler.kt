package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UploadWorkScheduler {

    suspend fun enqueueBatch(context: Context, items: List<UploadWork.BatchItem>): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        sweepStaleCache(app)
        val batchId = UploadWork.newBatchId()
        val upload = OneTimeWorkRequestBuilder<FileUploadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    UploadWork.IN_BATCH_ID to batchId,
                    UploadWork.IN_TOTAL to items.size,
                    UploadWork.IN_NAMES to items.map { it.name }.toTypedArray(),
                    UploadWork.IN_MIMES to items.map { it.mime }.toTypedArray(),
                    UploadWork.IN_PATHS to items.map { it.path }.toTypedArray(),
                    UploadWork.OUT_PATHS to items.map { it.path }.toTypedArray(),
                    UploadWork.OUT_NAMES to items.map { it.name }.toTypedArray(),
                    UploadWork.OUT_MIMES to items.map { it.mime }.toTypedArray(),
                ),
            )
            .build()
        val poll = OneTimeWorkRequestBuilder<AnalysisPollWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        val generate = OneTimeWorkRequestBuilder<GenerateQuestionsWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(app)
            .beginUniqueWork(UploadWork.batchName(batchId), ExistingWorkPolicy.KEEP, upload)
            .then(poll)
            .then(generate)
            .enqueue()
        val prefs = UploadPrefs(app)
        prefs.setActiveBatchId(batchId)
        prefs.clearFailed()
        prefs.saveFailed(
            FailedBatch(
                batchId = batchId,
                paths = items.map { it.path },
                names = items.map { it.name },
                mimes = items.map { it.mime },
            ),
        )
        batchId
    }

    suspend fun enqueuePersist(
        context: Context,
        batchId: String,
        generatedFile: String,
        discardedIds: Set<String>,
    ): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val name = UploadWork.persistName(batchId)
        val request = OneTimeWorkRequestBuilder<PersistCurationWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    UploadWork.IN_BATCH_ID to batchId,
                    UploadWork.IN_GENERATED_FILE to generatedFile,
                    UploadWork.IN_DISCARD_IDS to discardedIds.toTypedArray(),
                ),
            )
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        name
    }

    suspend fun resumeFailed(context: Context): String? {
        val app = context.applicationContext
        val failed = UploadPrefs(app).readFailed() ?: return null
        return withContext(Dispatchers.IO) {
            resumeWith(app, failed)
        }
    }

    private suspend fun resumeWith(app: Context, failed: FailedBatch): String? {
        UploadPrefs(app).clearFailed()
        sweepStaleCache(app, keepBatchId = failed.batchId)
        val count = minOf(failed.paths.size, failed.names.size, failed.mimes.size)
        val alive = (0 until count).filter { File(failed.paths[it]).exists() }
        val paths = alive.map { failed.paths[it] }
        val names = alive.map { failed.names[it] }
        val mimes = alive.map { failed.mimes[it] }
        val docIds = failed.docIds.take(paths.size)
        return when {
            paths.isNotEmpty() -> {
                val batchId = failed.batchId
                val upload = OneTimeWorkRequestBuilder<FileUploadWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        workDataOf(
                            UploadWork.IN_BATCH_ID to batchId,
                            UploadWork.IN_TOTAL to paths.size,
                            UploadWork.IN_RESUME to true,
                            UploadWork.IN_NAMES to names.toTypedArray(),
                            UploadWork.IN_MIMES to mimes.toTypedArray(),
                            UploadWork.IN_PATHS to paths.toTypedArray(),
                            UploadWork.OUT_PATHS to paths.toTypedArray(),
                            UploadWork.OUT_NAMES to names.toTypedArray(),
                            UploadWork.OUT_MIMES to mimes.toTypedArray(),
                            UploadWork.IN_DOC_IDS to docIds.toTypedArray(),
                            UploadWork.OUT_DOC_IDS to docIds.toTypedArray(),
                            UploadWork.IN_GENERATED_DOC_IDS to failed.generatedDocIds.toTypedArray(),
                        ),
                    )
                    .build()
                    val poll = OneTimeWorkRequestBuilder<AnalysisPollWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    val generate = OneTimeWorkRequestBuilder<GenerateQuestionsWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    WorkManager.getInstance(app)
                        .beginUniqueWork(UploadWork.batchName(batchId), ExistingWorkPolicy.KEEP, upload)
                        .then(poll)
                        .then(generate)
                        .enqueue()
                    UploadPrefs(app).setActiveBatchId(batchId)
                    batchId
            }
            docIds.isNotEmpty() -> {
                resumePoll(app, failed.batchId, names, mimes, docIds, failed.generatedDocIds)
            }
            else -> null
        }
    }

    private suspend fun resumePoll(
        app: Context,
        batchId: String,
        names: List<String>,
        mimes: List<String>,
        docIds: List<String>,
        generatedIds: List<String> = emptyList(),
    ): String {
        val displayNames = names.ifEmpty { docIds.map { "File" } }
        val poll = OneTimeWorkRequestBuilder<AnalysisPollWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    UploadWork.IN_BATCH_ID to batchId,
                    UploadWork.IN_TOTAL to maxOf(displayNames.size, docIds.size),
                    UploadWork.IN_NAMES to displayNames.toTypedArray(),
                    UploadWork.IN_MIMES to mimes.toTypedArray(),
                    UploadWork.IN_DOC_IDS to docIds.toTypedArray(),
                    UploadWork.OUT_DOC_IDS to docIds.toTypedArray(),
                    UploadWork.IN_GENERATED_DOC_IDS to generatedIds.toTypedArray(),
                ),
            )
            .build()
        val generate = OneTimeWorkRequestBuilder<GenerateQuestionsWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(app)
            .beginUniqueWork(UploadWork.batchName(batchId), ExistingWorkPolicy.KEEP, poll)
            .then(generate)
            .enqueue()
        UploadPrefs(app).setActiveBatchId(batchId)
        return batchId
    }

    fun cancelBatch(context: Context, batchId: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UploadWork.batchName(batchId))
    }

    fun sweepStaleCache(context: Context, keepBatchId: String? = null) {
        val threshold = System.currentTimeMillis() - STALE_AGE_MS
        context.applicationContext.cacheDir.listFiles { file ->
            file.isDirectory && file.name.startsWith(BATCH_DIR_PREFIX) && file.name != UploadWork.batchDirName(keepBatchId ?: "")
        }?.forEach { dir ->
            if (dir.lastModified() < threshold) {
                dir.deleteRecursively()
            }
        }
        context.applicationContext.cacheDir.listFiles { file ->
            file.isFile && file.name.startsWith(BATCH_DIR_PREFIX) && file.name.endsWith(GENERATED_SUFFIX) && file.lastModified() < threshold
        }?.forEach { it.delete() }
    }

    private const val STALE_AGE_MS = 48 * 60 * 60 * 1000L
    private const val BATCH_DIR_PREFIX = "upload_"
    private const val GENERATED_SUFFIX = "_generated.json"
}
