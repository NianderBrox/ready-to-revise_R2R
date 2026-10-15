package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.r2r.readytorevise.data.repository.SessionExpiredException
import com.r2r.readytorevise.di.DefaultAppContainer
import com.r2r.readytorevise.domain.repository.DocumentsRepository
import com.r2r.readytorevise.notification.UploadNotifications
import java.io.File
import java.io.InterruptedIOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val completedIds = ArrayList<String>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val batchId = inputData.getString(UploadWork.IN_BATCH_ID)
            ?: return@withContext fail("Missing batch", emptyList())
        val paths = inputData.getStringArray(UploadWork.IN_PATHS) ?: emptyArray()
        val names = inputData.getStringArray(UploadWork.IN_NAMES) ?: emptyArray()
        val mimes = inputData.getStringArray(UploadWork.IN_MIMES) ?: emptyArray()
        val total = inputData.getInt(UploadWork.IN_TOTAL, paths.size)
        if (paths.isEmpty() || paths.size != names.size || paths.size != mimes.size) {
            return@withContext fail("Nothing to upload", emptyList(), batchId)
        }
        return@withContext try {
        setForeground(
            UploadNotifications.foregroundInfo(
                applicationContext, batchId,
                UploadWork.STAGE_UPLOAD, 0, total, 0, "Uploading ${names[0]}…",
            ),
        )
        UploadNotifications.dismissResult(applicationContext)
        val repository = DefaultAppContainer(applicationContext).documentsRepository
        completedIds.addAll(inputData.getStringArray(UploadWork.IN_DOC_IDS)?.toList().orEmpty())
        if (inputData.getBoolean(UploadWork.IN_RESUME, false)) {
            adoptAnalyzed(repository, batchId, paths.toList(), names.toList(), mimes.toList())
        }
        saveDescriptor(batchId)
        val start = completedIds.size.coerceIn(0, paths.size)
        val totalBytes = paths.drop(start).sumOf { File(it).length().coerceAtLeast(0L) }
        var uploadedBefore = 0L
        var lastNotifyAt = 0L
        for (index in start until paths.size) {
            val file = File(paths[index])
            if (!file.exists()) {
                return@withContext fail("Missing file ${names[index]}", listOf(names[index]), batchId)
            }
            report(applicationContext, batchId, index, total, percent(uploadedBefore, totalBytes), "Uploading ${names[index]}…")
            val result = try {
                repository.upload(file, mimes[index], { isStopped }) { sent, _ ->
                    val overall = uploadedBefore + sent
                    val now = System.currentTimeMillis()
                    if (now - lastNotifyAt > 500 || overall >= totalBytes) {
                        lastNotifyAt = now
                        UploadNotifications.showProgress(
                            applicationContext, batchId, index, total,
                            percent(overall, totalBytes), "Uploading ${names[index]}…",
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                return@withContext fail("Upload interrupted (${t.javaClass.simpleName})", listOf(names[index]), batchId)
            }
            val dto = result.getOrElse { error ->
                if (error is SessionExpiredException || isStopped) {
                    if (error is SessionExpiredException) {
                        return@withContext fail(error.message ?: "Session expired", listOf(names[index]), batchId, loginRequired = true)
                    }
                    return@withContext cancelled(batchId)
                }
                if (error is InterruptedIOException) {
                    return@withContext cancelled(batchId)
                }
                return@withContext fail(error.message ?: "Upload failed", listOf(names[index]), batchId)
            }
            completedIds += dto.documentId
            saveDescriptor(batchId)
            uploadedBefore += file.length().coerceAtLeast(0L)
            report(applicationContext, batchId, index + 1, total, percent(uploadedBefore, totalBytes), "Uploaded ${names[index]}")
        }
        Result.success(
            UploadWork.forward(
                inputData,
                UploadWork.IN_DOC_IDS to completedIds.toTypedArray(),
                UploadWork.OUT_DOC_IDS to completedIds.toTypedArray(),
            ),
        )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            fail("Unexpected error", emptyList(), inputData.getString(UploadWork.IN_BATCH_ID))
        }
    }

    private suspend fun saveDescriptor(batchId: String) {
        UploadPrefs(applicationContext).saveFailed(
            FailedBatch(
                batchId = batchId,
                uris = inputData.getStringArray(UploadWork.IN_URIS)?.toList().orEmpty(),
                paths = inputData.getStringArray(UploadWork.IN_PATHS)?.toList().orEmpty(),
                names = inputData.getStringArray(UploadWork.IN_NAMES)?.toList().orEmpty(),
                mimes = inputData.getStringArray(UploadWork.IN_MIMES)?.toList().orEmpty(),
                docIds = completedIds.toList(),
            ),
        )
    }

    private suspend fun adoptAnalyzed(
        repository: DocumentsRepository,
        batchId: String,
        paths: List<String>,
        names: List<String>,
        mimes: List<String>,
    ) {
        val remote = repository.getDocuments().getOrNull().orEmpty()
        if (remote.isEmpty()) {
            return
        }
        val now = System.currentTimeMillis()
        val start = completedIds.size.coerceIn(0, paths.size)
        for (index in start until paths.size) {
            val stagedName = File(paths[index]).name
            val match = remote.firstOrNull { doc ->
                doc.originalName == stagedName &&
                    doc.mimeType == mimes[index] &&
                    doc.status != UploadWork.STATUS_FAILED &&
                    isRecent(doc.createdAt, now)
            } ?: break
            completedIds += match.id
        }
        if (completedIds.isNotEmpty()) {
            saveDescriptor(batchId)
        }
    }

    private fun isRecent(createdAt: String, now: Long): Boolean {
        val instant = runCatching { java.time.Instant.parse(createdAt) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(createdAt).toInstant() }.getOrNull()
            ?: return false
        val age = now - instant.toEpochMilli()
        return age in 0..ADOPT_WINDOW_MS
    }

    private suspend fun report(context: Context, batchId: String, completed: Int, total: Int, percent: Int, message: String) {
        setProgress(
            workDataOf(
                UploadWork.P_STAGE to UploadWork.STAGE_UPLOAD,
                UploadWork.P_COMPLETED to completed,
                UploadWork.P_TOTAL to total,
                UploadWork.P_MESSAGE to message,
            ),
        )
        UploadNotifications.showProgress(context, batchId, completed, total, percent, message)
    }

    private fun percent(done: Long, total: Long): Int {
        if (total <= 0L) {
            return 0
        }
        return ((done * 100L) / total).coerceIn(0L, 100L).toInt()
    }

    private suspend fun saveDescriptor(batchId: String, docIds: List<String>) {
        UploadPrefs(applicationContext).saveFailed(
            FailedBatch(
                batchId = batchId,
                uris = inputData.getStringArray(UploadWork.IN_URIS)?.toList().orEmpty(),
                paths = inputData.getStringArray(UploadWork.IN_PATHS)?.toList().orEmpty(),
                names = inputData.getStringArray(UploadWork.IN_NAMES)?.toList().orEmpty(),
                mimes = inputData.getStringArray(UploadWork.IN_MIMES)?.toList().orEmpty(),
                docIds = docIds,
            ),
        )
    }

    private suspend fun fail(message: String, failedFiles: List<String>, batchId: String? = null, loginRequired: Boolean = false): Result {
        if (batchId != null) {
            saveDescriptor(batchId)
            UploadNotifications.showError(applicationContext, batchId, message, loginRequired)
        }
        return Result.failure(
            UploadWork.forward(
                inputData,
                UploadWork.OUT_ERROR to message,
                UploadWork.OUT_FAILED_FILES to failedFiles.toTypedArray(),
                UploadWork.OUT_LOGIN_REQUIRED to loginRequired,
            ),
        )
    }

    private suspend fun cancelled(batchId: String): Result {
        UploadNotifications.dismiss(applicationContext)
        return Result.failure(
            UploadWork.forward(
                inputData,
                UploadWork.OUT_ERROR to "Upload cancelled",
                UploadWork.OUT_FAILED_FILES to emptyArray<String>(),
                UploadWork.OUT_CANCELLED to true,
            ),
        )
    }

    private companion object {
        const val ADOPT_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}
