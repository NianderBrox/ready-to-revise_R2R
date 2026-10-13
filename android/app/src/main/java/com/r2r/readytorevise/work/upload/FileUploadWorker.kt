package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.r2r.readytorevise.data.repository.SessionExpiredException
import com.r2r.readytorevise.di.DefaultAppContainer
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
        setForeground(
            UploadNotifications.foregroundInfo(
                applicationContext, batchId,
                UploadWork.STAGE_UPLOAD, 0, total, 0, "Uploading ${names[0]}…",
            ),
        )
        val repository = DefaultAppContainer(applicationContext).documentsRepository
        val totalBytes = paths.sumOf { File(it).length().coerceAtLeast(0L) }
        var uploadedBefore = 0L
        var lastNotifyAt = 0L
        val docIds = ArrayList<String>(paths.size)
        for (index in paths.indices) {
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
                return@withContext fail("Upload interrupted", listOf(names[index]), batchId)
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
            docIds += dto.documentId
            uploadedBefore += file.length().coerceAtLeast(0L)
            report(applicationContext, batchId, index + 1, total, percent(uploadedBefore, totalBytes), "Uploaded ${names[index]}")
        }
        Result.success(
            UploadWork.forward(
                inputData,
                UploadWork.IN_DOC_IDS to docIds.toTypedArray(),
                UploadWork.OUT_DOC_IDS to docIds.toTypedArray(),
            ),
        )
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
            saveDescriptor(batchId, emptyList())
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
}
