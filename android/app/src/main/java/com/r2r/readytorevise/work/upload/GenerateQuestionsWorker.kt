package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.di.DefaultAppContainer
import com.r2r.readytorevise.notification.UploadNotifications
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GenerateQuestionsWorker(
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
        val doneIds = inputData.getStringArray(UploadWork.IN_GENERATED_DOC_IDS)?.toSet().orEmpty()
        val generatedIds = ArrayList<String>()
        return try {
        setForeground(
            UploadNotifications.foregroundInfo(
                applicationContext, batchId,
                UploadWork.STAGE_GENERATE, 0, total, null, "Generating questions…",
            ),
        )
        val repository = DefaultAppContainer(applicationContext).documentsRepository
        val allItems = ArrayList<StudyItemDto>()
        for (index in docIds.indices) {
            if (docIds[index] in doneIds) {
                continue
            }
            val label = names.getOrElse(index) { "File ${index + 1}" }
            val progress = workDataOf(
                UploadWork.P_STAGE to UploadWork.STAGE_GENERATE,
                UploadWork.P_COMPLETED to index,
                UploadWork.P_TOTAL to total,
                UploadWork.P_MESSAGE to "Generating questions for $label…",
            )
            setProgress(progress)
            UploadNotifications.showProgress(
                applicationContext, batchId,
                index, total, null, "Generating questions for $label…",
            )
            val questions = repository.generateQuestions(docIds[index]).getOrElse { error ->
                return fail(error.message ?: "Question generation failed", listOf(label), batchId, generatedIds.toList())
            }
            allItems += questions
            generatedIds += docIds[index]
            saveDescriptor(batchId, generatedIds.toList())
        }
        val outFile = File(applicationContext.cacheDir, UploadWork.batchDirName(batchId) + "_generated.json")
        try {
            outFile.writeText(json.encodeToString(allItems))
            } catch (t: Throwable) {
                return fail("Could not save results", names.toList(), batchId, generatedIds.toList())
            }
        UploadPrefs(applicationContext).clearFailed()
        UploadNotifications.showAwaitingSelection(applicationContext, batchId, allItems.size)
        Result.success(
            UploadWork.forward(
                inputData,
                UploadWork.OUT_GENERATED_FILE to outFile.absolutePath,
            ),
        )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            fail("Unexpected error", emptyList(), batchId, generatedIds.toList())
        }
    }

    private suspend fun saveDescriptor(batchId: String, generatedIds: List<String>) {
        UploadPrefs(applicationContext).saveFailed(
            FailedBatch(
                batchId = batchId,
                paths = inputData.getStringArray(UploadWork.IN_PATHS)?.toList().orEmpty(),
                names = inputData.getStringArray(UploadWork.IN_NAMES)?.toList().orEmpty(),
                mimes = inputData.getStringArray(UploadWork.IN_MIMES)?.toList().orEmpty(),
                docIds = inputData.getStringArray(UploadWork.IN_DOC_IDS)?.toList().orEmpty(),
                generatedDocIds = generatedIds,
            ),
        )
    }

    private suspend fun fail(message: String, failedFiles: List<String>, batchId: String? = null, generatedIds: List<String> = emptyList()): Result {
        if (batchId != null) {
            saveDescriptor(batchId, generatedIds)
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
        val json = Json { ignoreUnknownKeys = true }
    }
}
