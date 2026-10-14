package com.r2r.readytorevise.presentation.upload

import android.content.Context
import android.net.Uri
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.presentation.base.BaseViewModel
import com.r2r.readytorevise.notification.UploadNotifications
import com.r2r.readytorevise.work.upload.UploadPrefs
import com.r2r.readytorevise.work.upload.UploadWork
import com.r2r.readytorevise.work.upload.UploadWorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

class UploadViewModel(
    private val appContext: Context,
) : BaseViewModel<UploadUiState, UploadEvent, UploadEffect>(UploadUiState()) {

    private val workManager by lazy { WorkManager.getInstance(appContext) }
    private val uploadPrefs by lazy { UploadPrefs(appContext) }
    private val json = Json { ignoreUnknownKeys = true }

    private var observeJob: Job? = null
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            uploadPrefs.activeBatchId.first()?.let { observeBatch(it) }
        }
    }

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
            UploadEvent.RetryBatch -> retryBatch()
            UploadEvent.CancelActiveBatch -> cancelActiveBatch()
            is UploadEvent.ToggleDiscard -> toggleDiscard(event.studyItemId)
            UploadEvent.ConfirmCuration -> persistSelection(keepAll = false)
            UploadEvent.SkipCuration -> persistSelection(keepAll = true)
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
                statusLine = "Queued…",
                completedItems = 0,
                totalItems = items.size,
                generatedCount = 0,
                failureLines = emptyList(),
                generatedItems = emptyList(),
                discardedIds = emptySet(),
                curationBusy = false,
                generatedFilePath = null,
            )
        }

        viewModelScope.launch {
            val newBatchId = UploadWorkScheduler.enqueueBatch(
                appContext,
                items.map { UploadWork.BatchItem(it.uri.toString(), it.fileName, it.mimeType) },
            )
            updateState { copy(batchId = newBatchId) }
            observeBatch(newBatchId)
        }
    }

    private fun retryBatch() {
        if (currentState.isBusy) {
            return
        }

        viewModelScope.launch {
            val newBatchId = UploadWorkScheduler.resumeFailed(appContext)

            if (newBatchId == null) {
                sendEffect(UploadEffect.ShowToast("Nothing to retry"))
                return@launch
            }

            updateState {
                copy(
                    stage = UploadStage.WORKING,
                    statusLine = "Retrying…",
                    failureLines = emptyList(),
                    batchId = newBatchId,
                )
            }
            observeBatch(newBatchId)
        }
    }

    private fun cancelActiveBatch() {
        val batchId = currentState.batchId

        if (currentState.stage != UploadStage.WORKING || batchId == null) {
            return
        }

        viewModelScope.launch {
            WorkManager.getInstance(appContext).cancelUniqueWork(UploadWork.batchName(batchId))
            File(appContext.cacheDir, UploadWork.batchDirName(batchId)).deleteRecursively()
            File(appContext.cacheDir, UploadWork.batchDirName(batchId) + "_generated.json").delete()
            uploadPrefs.setActiveBatchId(null)
            UploadNotifications.dismiss(appContext)
            updateState { copy(stage = UploadStage.IDLE, statusLine = "", batchId = null) }
            sendEffect(UploadEffect.ShowToast("Upload cancelled"))
        }
    }

    private fun observeBatch(batchId: String) {        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(UploadWork.batchName(batchId)).collect { infos ->
                mapChainState(infos, batchId)
            }
        }
    }

    private suspend fun mapChainState(infos: List<WorkInfo>, batchId: String) {
        if (infos.isEmpty()) {
            return
        }

        val failed = infos.firstOrNull { it.state == WorkInfo.State.FAILED }

        if (failed != null) {
            val error = failed.outputData.getString(UploadWork.OUT_ERROR) ?: "Upload failed"
            val failedNames = failed.outputData.getStringArray(UploadWork.OUT_FAILED_FILES)?.toList().orEmpty()
            val cancelled = failed.outputData.getBoolean(UploadWork.OUT_CANCELLED, false)
            val loginRequired = failed.outputData.getBoolean(UploadWork.OUT_LOGIN_REQUIRED, false)

            if (cancelled) {
                uploadPrefs.setActiveBatchId(null)
                if (currentState.stage != UploadStage.IDLE) {
                    updateState { copy(stage = UploadStage.IDLE, statusLine = "", batchId = null) }
                    sendEffect(UploadEffect.ShowToast("Upload cancelled"))
                }
            } else {
                updateState {
                    copy(
                        stage = UploadStage.ERROR,
                        statusLine = if (loginRequired) {
                            "$error — please log in again"
                        } else {
                            error
                        },
                        failureLines = failedNames.map { "· $it" },
                    )
                }
            }
            return
        }

        if (infos.any { it.state == WorkInfo.State.CANCELLED }) {
            uploadPrefs.setActiveBatchId(null)
            if (currentState.stage != UploadStage.IDLE) {
                updateState { copy(stage = UploadStage.IDLE, statusLine = "", batchId = null) }
            }
            return
        }

        val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }

        if (running == null) {
            if (infos.all { it.state == WorkInfo.State.SUCCEEDED }) {
                val generatedFile = infos.firstNotNullOfOrNull {
                    it.outputData.getString(UploadWork.OUT_GENERATED_FILE)
                }

                if (generatedFile != null) {
                    val items = readGeneratedFile(generatedFile)

                    if (items != null) {
                        updateState {
                            copy(
                                stage = UploadStage.DONE,
                                statusLine = "${items.size} generated — discard anything off-topic",
                                completedItems = totalItems,
                                generatedCount = items.size,
                                generatedItems = items,
                                generatedFilePath = generatedFile,
                            )
                        }
                    } else {
                        updateState {
                            copy(
                                stage = UploadStage.ERROR,
                                statusLine = "Could not read results",
                            )
                        }
                    }
                } else {
                    updateState {
                        copy(
                            stage = UploadStage.ERROR,
                            statusLine = "Upload finished without results",
                        )
                    }
                }
            } else {
                updateState { copy(stage = UploadStage.WORKING, statusLine = "Queued…") }
            }
            return
        }

        val progress = running.progress
        updateState {
            copy(
                stage = UploadStage.WORKING,
                completedItems = progress.getInt(UploadWork.P_COMPLETED, completedItems),
                totalItems = progress.getInt(UploadWork.P_TOTAL, totalItems),
                statusLine = progress.getString(UploadWork.P_MESSAGE) ?: "Working…",
            )
        }
    }

    private suspend fun readGeneratedFile(path: String): List<StudyItemDto>? {
        return withContext(Dispatchers.IO) {
            try {
                json.decodeFromString<List<StudyItemDto>>(File(path).readText())
            } catch (t: Throwable) {
                null
            }
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

    private fun persistSelection(keepAll: Boolean) {
        val state = currentState
        val toDiscard = if (keepAll) emptySet() else state.discardedIds

        if (state.curationBusy) {
            return
        }

        if (state.stage != UploadStage.DONE || state.generatedFilePath == null) {
            if (keepAll) {
                viewModelScope.launch {
                    sendEffect(UploadEffect.ShowToast("All generated questions kept"))
                    sendEffect(UploadEffect.NavigateToDashboard)
                }
            }
            return
        }

        updateState {
            copy(
                curationBusy = true,
                statusLine = "Saving…",
            )
        }

        viewModelScope.launch {
            val batchId = state.batchId ?: UploadWork.newBatchId()
            val name = UploadWorkScheduler.enqueuePersist(
                appContext,
                batchId,
                state.generatedFilePath,
                toDiscard,
            )
            observePersist(name)
        }
    }

    private fun observePersist(name: String) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(name).collect { infos ->
                val info = infos.firstOrNull() ?: return@collect

                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val kept = info.outputData.getInt(UploadWork.OUT_KEPT, currentState.keptCount)
                        uploadPrefs.setActiveBatchId(null)
                        uploadPrefs.clearFailed()
                        updateState {
                            copy(
                                curationBusy = false,
                                statusLine = "",
                                images = emptyList(),
                                files = emptyList(),
                                generatedItems = emptyList(),
                                discardedIds = emptySet(),
                                generatedFilePath = null,
                                batchId = null,
                                stage = UploadStage.IDLE,
                            )
                        }
                        sendEffect(UploadEffect.ShowToast("$kept question${if (kept == 1) "" else "s"} saved"))
                        sendEffect(UploadEffect.NavigateToDashboard)
                        persistJob?.cancel()
                    }
                    WorkInfo.State.FAILED -> {
                        updateState { copy(curationBusy = false) }
                        val error = info.outputData.getString(UploadWork.OUT_ERROR) ?: "Couldn't save"
                        sendEffect(UploadEffect.ShowToast(error))
                    }
                    WorkInfo.State.CANCELLED -> {
                        updateState { copy(curationBusy = false) }
                    }
                    else -> Unit
                }
            }
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