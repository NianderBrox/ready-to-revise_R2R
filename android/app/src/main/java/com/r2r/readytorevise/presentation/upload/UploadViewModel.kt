package com.r2r.readytorevise.presentation.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.data.remote.dto.DocumentDto
import com.r2r.readytorevise.domain.repository.DocumentsRepository
import com.r2r.readytorevise.domain.repository.StudyItemsRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class UploadViewModel(
    private val documentsRepository: DocumentsRepository,
    private val studyItemsRepository: StudyItemsRepository,
    private val appContext: Context,
) : BaseViewModel<UploadUiState, UploadEvent, UploadEffect>(UploadUiState()) {

    override fun onEvent(event: UploadEvent) {
        when (event) {
            is UploadEvent.AddImage -> addImage(event.uri)
            is UploadEvent.ReplaceImage -> replaceImage(event.index, event.uri)
            is UploadEvent.RemoveImage -> removeImage(event.index)
            UploadEvent.ClearImages -> updateState { copy(images = emptyList()) }
            is UploadEvent.AddFiles -> addFiles(event.files)
            is UploadEvent.SetFiles -> setFiles(event.files)
            is UploadEvent.RemoveFile -> removeFile(event.index)
            UploadEvent.Submit -> submitAll()
            is UploadEvent.ToggleDiscard -> toggleDiscard(event.studyItemId)
            UploadEvent.ConfirmCuration -> confirmCuration()
            UploadEvent.SkipCuration -> skipCuration()
        }
    }

    private fun addImage(uri: Uri) {
        updateState {
            if (images.size < MAX_IMAGES) copy(images = images + uri) else this
        }
    }

    private fun replaceImage(index: Int, uri: Uri) {
        updateState {
            if (index in images.indices) {
                copy(images = images.toMutableList().apply { set(index, uri) })
            } else {
                this
            }
        }
    }

    private fun removeImage(index: Int) {
        updateState {
            if (index in images.indices) {
                copy(images = images.filterIndexed { i, _ -> i != index })
            } else {
                this
            }
        }
    }

    private fun addFiles(files: List<UploadedFile>) {
        updateState {
            copy(files = this.files + files.take(MAX_FILES - this.files.size))
        }
    }

    private fun setFiles(files: List<UploadedFile>) {
        updateState {
            copy(files = files.take(MAX_FILES))
        }
    }

    private fun removeFile(index: Int) {
        updateState {
            if (index in files.indices) {
                copy(files = files.filterIndexed { i, _ -> i != index })
            } else {
                this
            }
        }
    }

    private fun submitAll() {
        val snapshot = currentState

        if (snapshot.isBusy || !snapshot.canSubmit) {
            return
        }

        val items = buildQueue(snapshot)

        updateState {
            copy(
                stage = UploadStage.WORKING,
                statusLine = "Starting upload…",
                completedItems = 0,
                totalItems = items.size,
                generatedCount = 0,
                failureLines = emptyList(),
                errorCountdownSeconds = null,
                generatedItems = emptyList(),
                discardedIds = emptySet(),
                curationBusy = false,
            )
        }

        viewModelScope.launch {
            val failures = mutableListOf<String>()
            var generated = 0

            for ((index, item) in items.withIndex()) {
                updateState {
                    copy(
                        completedItems = index,
                        statusLine = "Uploading ${item.fileName}…",
                    )
                }

                val outcome = processItem(item)

                if (outcome != null) {
                    generated += outcome
                } else {
                    failures += item.fileName
                }

                updateState { copy(completedItems = index + 1) }
            }

            finishRun(items.size, generated, failures)
        }
    }

    private suspend fun processItem(item: UploadedFile): Int? {
        var cacheFile: File? = null

        return try {
            val file = copyToCache(item)

            if (file == null) {
                null
            } else {
                cacheFile = file

                updateState {
                    copy(statusLine = "Analyzing ${item.fileName}…")
                }

                val document = documentsRepository.upload(file, item.mimeType)
                    .getOrElse { return null }

                val readyDocument = awaitReady(document.documentId, document.status)

                if (readyDocument == null || readyDocument.status == STATUS_FAILED) {
                    return null
                }

                updateState {
                    copy(statusLine = "Generating questions for ${item.fileName}…")
                }

                val questions = documentsRepository.generateQuestions(readyDocument.id)
                    .getOrElse { return null }

                updateState {
                    copy(generatedItems = generatedItems + questions)
                }

                questions.size
            }
        } finally {
            cacheFile?.delete()
        }
    }

    private suspend fun awaitReady(
        documentId: String,
        initialStatus: String,
    ): DocumentDto? {
        if (initialStatus == STATUS_READY) {
            return documentsRepository.getDocument(documentId).getOrNull()
        }

        var current: DocumentDto? = documentsRepository.getDocument(documentId).getOrNull()

        repeat(POLL_ATTEMPTS) {
            if (current?.status == STATUS_READY || current?.status == STATUS_FAILED) {
                return current
            }

            delay(POLL_INTERVAL_MS)

            current = documentsRepository.getDocument(documentId).getOrNull()
        }

        return current?.takeIf { it.status == STATUS_READY || it.status == STATUS_FAILED }
    }

    private suspend fun copyToCache(item: UploadedFile): File? {
        return withContext(Dispatchers.IO) {
            try {
                appContext.contentResolver.openInputStream(item.uri)?.use { input ->
                    val safeName = item.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    File.createTempFile("upload_", "_$safeName", appContext.cacheDir)
                        .apply { outputStream().use { output -> input.copyTo(output) } }
                }
            } catch (t: Throwable) {
                null
            }
        }
    }

    private suspend fun finishRun(
        total: Int,
        generated: Int,
        failures: List<String>,
    ) {
        val succeededAny = failures.size < total

        if (succeededAny) {
            updateState {
                copy(
                    images = emptyList(),
                    files = emptyList(),
                )
            }
        }

        val message = when {
            failures.isEmpty() -> "$generated question${if (generated == 1) "" else "s"} created"
            failures.size == total -> "Upload failed"
            else -> "$generated created · ${failures.size} failed"
        }

        updateState {
            copy(
                stage = if (failures.size == total) UploadStage.ERROR else UploadStage.DONE,
                statusLine = message,
                failureLines = failures.map { "· $it" },
                errorCountdownSeconds = null,
            )
        }

        if (failures.size == total) {
            for (remaining in ERROR_AUTO_NAVIGATE_SECONDS downTo 1) {
                updateState { copy(errorCountdownSeconds = remaining) }
                delay(1000L)
            }
        }

        if (failures.size == total) {
            for (remaining in ERROR_AUTO_NAVIGATE_SECONDS downTo 1) {
                updateState { copy(errorCountdownSeconds = remaining) }
                delay(1000L)
            }

            sendEffect(UploadEffect.ShowToast(message))
            sendEffect(UploadEffect.NavigateToDashboard)
        } else {
            sendEffect(UploadEffect.ShowToast(message))
        }
    }

    private fun toggleDiscard(studyItemId: String) {
        updateState {
            copy(
                discardedIds = if (discardedIds.contains(studyItemId)) {
                    discardedIds - studyItemId
                } else {
                    discardedIds + studyItemId
                },
            )
        }
    }

    private fun confirmCuration() {
        val state = currentState

        if (state.curationBusy) {
            return
        }

        if (state.stage != UploadStage.DONE || state.discardedIds.isEmpty()) {
            skipCuration()
            return
        }

        updateState {
            copy(
                curationBusy = true,
                statusLine = "Removing discarded questions…",
            )
        }

        viewModelScope.launch {
            var failures = 0

            for (id in state.discardedIds) {
                studyItemsRepository.deleteStudyItem(id)
                    .onFailure { failures += 1 }
            }

            val kept = state.generatedItems.size -
                state.discardedIds.size + failures

            updateState { copy(curationBusy = false, statusLine = "") }

            val message = "$kept question${if (kept == 1) "" else "s"} saved" +
                if (failures > 0) " · ${failures} removals failed" else ""

            delay(2000L)

            sendEffect(UploadEffect.ShowToast(message))
            sendEffect(UploadEffect.NavigateToDashboard)
        }
    }

    private fun skipCuration() {
        viewModelScope.launch {
            sendEffect(UploadEffect.ShowToast("All generated questions kept"))
            sendEffect(UploadEffect.NavigateToDashboard)
        }
    }

    private fun buildQueue(snapshot: UploadUiState): List<UploadedFile> {
        val queue = mutableListOf<UploadedFile>()

        snapshot.images.forEachIndexed { index, uri ->
            queue += UploadedFile(
                uri = uri,
                fileName = "image_${index + 1}.jpg",
                type = FileType.IMAGE,
                mimeType = guessImageMime(appContext, uri),
            )
        }

        snapshot.files.forEach { file ->
            queue += file.copy(mimeType = file.mimeType.ifBlank { guessMime(file.fileName) })
        }

        return queue
    }

    private companion object {
        const val MAX_IMAGES = 5
        const val MAX_FILES = 10
        const val POLL_INTERVAL_MS = 3_000L
        const val POLL_ATTEMPTS = 40
        const val STATUS_READY = "READY"
        const val STATUS_FAILED = "FAILED"
        const val ERROR_AUTO_NAVIGATE_SECONDS = 5
    }
}

private fun guessImageMime(context: Context, uri: Uri): String {
    return context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") }
        ?: "image/jpeg"
}

internal fun guessMime(fileName: String): String {
    val lower = fileName.lowercase(Locale.US)

    return when {
        lower.endsWith(".pdf") -> "application/pdf"
        lower.endsWith(".docx") ->
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        lower.endsWith(".doc") -> "application/msword"
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        else -> "application/octet-stream"
    }
}
