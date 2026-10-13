package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File

object UploadWorkScheduler {

    suspend fun enqueueBatch(context: Context, items: List<UploadWork.BatchItem>): String {
        val app = context.applicationContext
        sweepStaleCache(app)
        val batchId = UploadWork.newBatchId()
        val prepare = OneTimeWorkRequestBuilder<PrepareUploadWorker>()
            .setInputData(
                workDataOf(
                    UploadWork.IN_BATCH_ID to batchId,
                    UploadWork.IN_TOTAL to items.size,
                    UploadWork.IN_URIS to items.map { it.uri }.toTypedArray(),
                    UploadWork.IN_NAMES to items.map { it.name }.toTypedArray(),
                    UploadWork.IN_MIMES to items.map { it.mime }.toTypedArray(),
                ),
            )
            .build()
        val upload = OneTimeWorkRequestBuilder<FileUploadWorker>().build()
        val poll = OneTimeWorkRequestBuilder<AnalysisPollWorker>().build()
        val generate = OneTimeWorkRequestBuilder<GenerateQuestionsWorker>().build()
        WorkManager.getInstance(app)
            .beginUniqueWork(UploadWork.batchName(batchId), ExistingWorkPolicy.KEEP, prepare)
            .then(upload)
            .then(poll)
            .then(generate)
            .enqueue()
        val prefs = UploadPrefs(app)
        prefs.setActiveBatchId(batchId)
        prefs.clearFailed()
        return batchId
    }

    suspend fun enqueuePersist(
        context: Context,
        batchId: String,
        generatedFile: String,
        discardedIds: Set<String>,
    ): String {
        val app = context.applicationContext
        val name = UploadWork.persistName(batchId)
        val request = OneTimeWorkRequestBuilder<PersistCurationWorker>()
            .setInputData(
                workDataOf(
                    UploadWork.IN_BATCH_ID to batchId,
                    UploadWork.IN_GENERATED_FILE to generatedFile,
                    UploadWork.IN_DISCARD_IDS to discardedIds.toTypedArray(),
                ),
            )
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        return name
    }

    suspend fun resumeFailed(context: Context): String? {
        val app = context.applicationContext
        val failed = UploadPrefs(app).readFailed() ?: return null
        UploadPrefs(app).clearFailed()
        sweepStaleCache(app, keepBatchId = failed.batchId)
        val docIds = failed.docIds
        val paths = failed.paths.filter { File(it).exists() }
        return when {
            docIds.isNotEmpty() -> {
                val batchId = failed.batchId
                val total = failed.names.ifEmpty { docIds.map { "File" } }.size
                val poll = OneTimeWorkRequestBuilder<AnalysisPollWorker>()
                    .setInputData(
                        workDataOf(
                            UploadWork.IN_BATCH_ID to batchId,
                            UploadWork.IN_TOTAL to total,
                            UploadWork.IN_NAMES to failed.names.ifEmpty { docIds.map { "File" } }.toTypedArray(),
                            UploadWork.IN_MIMES to failed.mimes.toTypedArray(),
                            UploadWork.IN_DOC_IDS to docIds.toTypedArray(),
                            UploadWork.OUT_DOC_IDS to docIds.toTypedArray(),
                        ),
                    )
                    .build()
                val generate = OneTimeWorkRequestBuilder<GenerateQuestionsWorker>().build()
                WorkManager.getInstance(app)
                    .beginUniqueWork(UploadWork.batchName(batchId), ExistingWorkPolicy.KEEP, poll)
                    .then(generate)
                    .enqueue()
                UploadPrefs(app).setActiveBatchId(batchId)
                batchId
            }
            paths.isNotEmpty() && paths.size == failed.names.size && paths.size == failed.mimes.size -> {
                val batchId = failed.batchId
                val upload = OneTimeWorkRequestBuilder<FileUploadWorker>()
                    .setInputData(
                        workDataOf(
                            UploadWork.IN_BATCH_ID to batchId,
                            UploadWork.IN_TOTAL to paths.size,
                            UploadWork.IN_URIS to failed.uris.toTypedArray(),
                            UploadWork.IN_NAMES to failed.names.toTypedArray(),
                            UploadWork.IN_MIMES to failed.mimes.toTypedArray(),
                            UploadWork.IN_PATHS to paths.toTypedArray(),
                            UploadWork.OUT_PATHS to paths.toTypedArray(),
                            UploadWork.OUT_NAMES to failed.names.toTypedArray(),
                            UploadWork.OUT_MIMES to failed.mimes.toTypedArray(),
                        ),
                    )
                    .build()
                val poll = OneTimeWorkRequestBuilder<AnalysisPollWorker>().build()
                val generate = OneTimeWorkRequestBuilder<GenerateQuestionsWorker>().build()
                WorkManager.getInstance(app)
                    .beginUniqueWork(UploadWork.batchName(batchId), ExistingWorkPolicy.KEEP, upload)
                    .then(poll)
                    .then(generate)
                    .enqueue()
                UploadPrefs(app).setActiveBatchId(batchId)
                batchId
            }
            failed.uris.isNotEmpty() -> {
                enqueueBatch(
                    app,
                    failed.uris.mapIndexed { index, uri ->
                        UploadWork.BatchItem(
                            uri = uri,
                            name = failed.names.getOrElse(index) { "file" },
                            mime = failed.mimes.getOrElse(index) { "application/octet-stream" },
                        )
                    },
                )
            }
            else -> null
        }
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
