package com.r2r.readytorevise.work.upload

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.r2r.readytorevise.notification.UploadNotifications
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrepareUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val batchId = inputData.getString(UploadWork.IN_BATCH_ID)
            ?: return fail("Missing batch", emptyList())
        val uris = inputData.getStringArray(UploadWork.IN_URIS) ?: emptyArray()
        val names = inputData.getStringArray(UploadWork.IN_NAMES) ?: emptyArray()
        val mimes = inputData.getStringArray(UploadWork.IN_MIMES) ?: emptyArray()
        val total = inputData.getInt(UploadWork.IN_TOTAL, uris.size)
        if (uris.isEmpty() || uris.size != names.size || uris.size != mimes.size) {
            return fail("Nothing to upload", emptyList(), batchId)
        }
        setForeground(
            UploadNotifications.foregroundInfo(
                applicationContext, batchId,
                UploadWork.STAGE_PREPARE, 0, total, null, "Preparing files…",
            ),
        )
        val dir = File(applicationContext.cacheDir, UploadWork.batchDirName(batchId))
        if (!dir.exists() && !dir.mkdirs()) {
            return fail("Could not prepare storage", names.toList(), batchId)
        }
        val paths = ArrayList<String>(uris.size)
        for (index in uris.indices) {
            val safeName = names[index].replace(Regex("[^A-Za-z0-9._-]"), "_")
            val out = File(dir, safeName)
            val copied = withContext(Dispatchers.IO) {
                try {
                    applicationContext.contentResolver.openInputStream(Uri.parse(uris[index]))?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    } != null
                } catch (t: Throwable) {
                    out.delete()
                    false
                }
            }
            if (!copied) {
                return fail("Could not read ${names[index]}", names.toList(), batchId)
            }
            paths += out.absolutePath
            val progress = workDataOf(
                UploadWork.P_STAGE to UploadWork.STAGE_PREPARE,
                UploadWork.P_COMPLETED to (index + 1),
                UploadWork.P_TOTAL to total,
                UploadWork.P_MESSAGE to "Preparing ${names[index]}…",
            )
            setProgress(progress)
            UploadNotifications.showProgress(
                applicationContext, batchId,
                index + 1, total, null, "Preparing ${names[index]}…",
            )
        }
        UploadPrefs(applicationContext).clearFailed()
        return Result.success(
            UploadWork.forward(
                inputData,
                UploadWork.IN_PATHS to paths.toTypedArray(),
                UploadWork.OUT_PATHS to paths.toTypedArray(),
                UploadWork.OUT_NAMES to names,
                UploadWork.OUT_MIMES to mimes,
            ),
        )
    }

    private suspend fun fail(message: String, failedFiles: List<String>, batchId: String? = null): Result {
        if (batchId != null) {
            UploadPrefs(applicationContext).saveFailed(
                FailedBatch(
                    batchId = batchId,
                    uris = inputData.getStringArray(UploadWork.IN_URIS)?.toList().orEmpty(),
                    names = inputData.getStringArray(UploadWork.IN_NAMES)?.toList().orEmpty(),
                    mimes = inputData.getStringArray(UploadWork.IN_MIMES)?.toList().orEmpty(),
                ),
            )
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
}
