package com.r2r.readytorevise.presentation.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.notification.UploadNotifications
import com.r2r.readytorevise.presentation.base.BaseViewModel
import com.r2r.readytorevise.presentation.settings.AutoAcceptMode
import com.r2r.readytorevise.presentation.settings.SettingsPreferences
import com.r2r.readytorevise.work.upload.UploadPrefs
import com.r2r.readytorevise.work.upload.UploadWork
import com.r2r.readytorevise.work.upload.UploadWorkScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val settingsPrefs by lazy { SettingsPreferences(appContext) }
    private val json = Json { ignoreUnknownKeys = true }

    private var observeJob: Job? = null
    private var persistJob: Job? = null
    private var autoAcceptedPath: String? = null

    init {
        viewModelScope.launch {
            uploadPrefs.activeBatchId.first()?.let { observeBatch(it) }
        }
    }

    override fun onEvent(event: UploadEvent) {
        when (event) {
            is UploadEvent.AddImage -> stageImage(event.uri)
            is UploadEvent.ReplaceImage -> replaceImage(event.index, event.uri)
            is UploadEvent.RemoveImage -> removeImage(event.index)
            UploadEvent.ClearImages -> clearImages()
            is UploadEvent.AddFiles -> stageFiles(event.files)
            is UploadEvent.SetFiles -> replaceFiles(event.files)
            is UploadEvent.RemoveFile -> removeFile(event.index)
            UploadEvent.Submit -> submitAll()
            UploadEvent.RetryBatch -> retryBatch()
            UploadEvent.CancelActiveBatch -> cancelActiveBatch()
            is UploadEvent.ToggleDiscard -> toggleDiscard(event.studyItemId)
            UploadEvent.ConfirmCuration -> persistSelection(keepAll = false)
            UploadEvent.SkipCuration -> persistSelection(keepAll = true)
        }
    }

    private fun stageImage(uri: Uri) {
        if (currentState.images.size >= MAX_IMAGES) {
            return
        }
        updateState { copy(stagingInFlight = stagingInFlight + 1, stagingError = null) }
        viewModelScope.launch {
            val path = copyUriToStaging(uri, "img_${System.nanoTime()}.jpg")
            if (path != null) {
                updateState {
                    copy(
                        images = images + uri,
                        stagedImagePaths = stagedImagePaths + (uri.toString() to path),
                        stagingInFlight = (stagingInFlight - 1).coerceAtLeast(0),
                    )
                }
            } else {
                updateState {
                    copy(
                        stagingInFlight = (stagingInFlight - 1).coerceAtLeast(0),
                        stagingError = "Couldn't read that image",
                    )
                }
            }
        }
    }

    private fun replaceImage(index: Int, uri: Uri) {
        val snapshot = currentState
        if (index !in snapshot.images.indices) {
            return
        }
        val oldKey = snapshot.images[index].toString()
        val oldPath = snapshot.stagedImagePaths[oldKey]
        updateState { copy(stagingInFlight = stagingInFlight + 1, stagingError = null) }
        viewModelScope.launch {
            val path = copyUriToStaging(uri, "img_${System.nanoTime()}.jpg")
            if (path != null && index in currentState.images.indices) {
                deleteQuietly(oldPath)
                updateState {
                    copy(
                        images = images.toMutableList().apply { set(index, uri) },
                        stagedImagePaths = stagedImagePaths - oldKey + (uri.toString() to path),
                        stagingInFlight = (stagingInFlight - 1).coerceAtLeast(0),
                    )
                }
            } else {
                deleteQuietly(path)
                updateState {
                    copy(
                        stagingInFlight = (stagingInFlight - 1).coerceAtLeast(0),
                        stagingError = "Couldn't read that image",
                    )
                }
            }
        }
    }

    private fun removeImage(index: Int) {
        val snapshot = currentState
        val key = snapshot.images.getOrNull(index)?.toString() ?: return
        val path = snapshot.stagedImagePaths[key]
        updateState {
            copy(
                images = images.filterIndexed { i, _ -> i != index },
                stagedImagePaths = stagedImagePaths - key,
            )
        }
        viewModelScope.launch { deleteQuietly(path) }
    }

    private fun clearImages() {
        val paths = currentState.stagedImagePaths.values.toList()
        updateState { copy(images = emptyList(), stagedImagePaths = emptyMap()) }
        viewModelScope.launch { deleteQuietly(paths) }
    }

    private fun stageFiles(incoming: List<UploadedFile>) {
        val capped = incoming.take(MAX_FILES - currentState.files.size)
        if (capped.isEmpty()) {
            return
        }
        updateState { copy(stagingInFlight = stagingInFlight + capped.size, stagingError = null) }
        viewModelScope.launch {
            val staged = capped.mapNotNull { stageDocFile(it) }
            updateState {
                copy(
                    files = files + staged,
                    stagingInFlight = (stagingInFlight - capped.size).coerceAtLeast(0),
                    stagingError = if (staged.size < capped.size) {
                        "Couldn't read some files"
                    } else {
                        stagingError
                    },
                )
            }
        }
    }

    private fun replaceFiles(incoming: List<UploadedFile>) {
        val capped = incoming.take(MAX_FILES)
        val previous = currentState.files.mapNotNull { it.cachePath }
        updateState { copy(files = emptyList(), stagingInFlight = stagingInFlight + capped.size, stagingError = null) }
        viewModelScope.launch {
            val staged = capped.mapNotNull { stageDocFile(it) }
            val fresh = staged.mapNotNull { it.cachePath }.toSet()
            deleteQuietly(previous.filter { it !in fresh })
            updateState {
                copy(
                    files = staged,
                    stagingInFlight = (stagingInFlight - capped.size).coerceAtLeast(0),
                    stagingError = if (staged.size < capped.size) {
                        "Couldn't read some files"
                    } else {
                        stagingError
                    },
                )
            }
        }
    }

    private fun removeFile(index: Int) {
        val path = currentState.files.getOrNull(index)?.cachePath ?: return
        updateState {
            copy(files = files.filterIndexed { i, _ -> i != index })
        }
        if (currentState.files.isEmpty()) {
            updateState { copy(stagingError = null) }
        }
        viewModelScope.launch { deleteQuietly(path) }
    }

    private suspend fun stageDocFile(file: UploadedFile): UploadedFile? {
        val safe = file.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val path = copyUriToStaging(file.uri, safe) ?: return null
        return file.copy(cachePath = path)
    }

    private suspend fun copyUriToStaging(uri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(appContext.cacheDir, STAGED_DIR).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val out = File(dir, "${System.nanoTime()}_$safe")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            out.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    private suspend fun deleteQuietly(path: String?) {
        if (path == null) {
            return
        }
        withContext(Dispatchers.IO) {
            runCatching { File(path).delete() }
        }
    }

    private suspend fun deleteQuietly(paths: List<String>) {
        paths.forEach { deleteQuietly(it) }
    }

    private fun submitAll() {
        val snapshot = currentState

        if (snapshot.isBusy) {
            return
        }

        if (!snapshot.canSubmit) {
            updateState {
                copy(
                    stage = UploadStage.IDLE,
                    statusLine = "",
                    failureLines = emptyList(),
                )
            }
            return
        }

        updateState {
            copy(
                stage = UploadStage.WORKING,
                statusLine = "Queued…",
                completedItems = 0,
                totalItems = snapshot.images.size + snapshot.files.size,
                generatedCount = 0,
                failureLines = emptyList(),
                generatedItems = emptyList(),
                discardedIds = emptySet(),
                curationBusy = false,
                generatedFilePath = null,
                stagingError = null,
            )
        }
        autoAcceptedPath = null

        viewModelScope.launch {
            try {
                val batch = withContext(Dispatchers.IO) { finalizeStaged(snapshot) }
                if (batch.isEmpty()) {
                    updateState {
                        copy(
                            stage = UploadStage.IDLE,
                            statusLine = "",
                            stagingError = "Files still preparing — try again",
                        )
                    }
                    return@launch
                }
                val newBatchId = UploadWorkScheduler.enqueueBatch(appContext, batch)
                updateState { copy(batchId = newBatchId, submittedPaths = batch.map { it.path }) }
                uploadPrefs.clearAutoResume()
                observeBatch(newBatchId)
                maybeRequestBatteryExemption()
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }
                updateState {
                    copy(
                        stage = UploadStage.ERROR,
                        statusLine = "Couldn't start upload",
                    )
                }
                sendEffect(UploadEffect.ShowToast("Couldn't start upload"))
            }
        }
    }

    private fun finalizeStaged(snapshot: UploadUiState): List<UploadWork.BatchItem> {
        val items = ArrayList<UploadWork.BatchItem>()
        snapshot.images.forEachIndexed { index, uri ->
            val staged = snapshot.stagedImagePaths[uri.toString()] ?: return@forEachIndexed
            val target = File(File(staged).parent ?: return@forEachIndexed, "image_${index + 1}.jpg")
            val finalPath = try {
                if (File(staged).renameTo(target)) {
                    target.absolutePath
                } else {
                    staged
                }
            } catch (t: Throwable) {
                staged
            }
            items += UploadWork.BatchItem(path = finalPath, name = "image_${index + 1}.jpg", mime = "image/jpeg")
        }
        snapshot.files.forEach { file ->
            val path = file.cachePath ?: return@forEach
            items += UploadWork.BatchItem(
                path = path,
                name = file.fileName,
                mime = file.mimeType.ifBlank { guessMime(file.fileName) },
            )
        }
        return items
    }

    private fun retryBatch() {
        if (currentState.isBusy) {
            return
        }

        viewModelScope.launch {
            try {
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
                uploadPrefs.clearAutoResume()
                observeBatch(newBatchId)
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }
                updateState {
                    copy(
                        stage = UploadStage.ERROR,
                        statusLine = "Couldn't start upload",
                    )
                }
                sendEffect(UploadEffect.ShowToast("Couldn't start upload"))
            }
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
            uploadPrefs.clearAutoResume()
            UploadNotifications.dismiss(appContext)
            updateState { copy(stage = UploadStage.IDLE, statusLine = "", batchId = null) }
            sendEffect(UploadEffect.ShowToast("Upload cancelled"))
        }
    }

    private suspend fun maybeRequestBatteryExemption() {
        val power = appContext.getSystemService(PowerManager::class.java) ?: return
        if (power.isIgnoringBatteryOptimizations(appContext.packageName)) {
            return
        }
        if (uploadPrefs.batteryPromptShown.first()) {
            return
        }
        uploadPrefs.setBatteryPromptShown()
        sendEffect(UploadEffect.ShowToast("Allow background activity so uploads keep running"))
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    suspend fun isBatchCompleted(batchId: String): Boolean {
        return uploadPrefs.isBatchCompleted(batchId)
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
            val rawError = failed.outputData.getString(UploadWork.OUT_ERROR)
            val error = rawError ?: "Upload interrupted — try again"
            val failedNames = failed.outputData.getStringArray(UploadWork.OUT_FAILED_FILES)?.toList().orEmpty()
            val cancelled = failed.outputData.getBoolean(UploadWork.OUT_CANCELLED, false)
            val loginRequired = failed.outputData.getBoolean(UploadWork.OUT_LOGIN_REQUIRED, false)

            if (cancelled) {
                uploadPrefs.setActiveBatchId(null)
                if (currentState.stage != UploadStage.IDLE) {
                    updateState { copy(stage = UploadStage.IDLE, statusLine = "", batchId = null) }
                    sendEffect(UploadEffect.ShowToast("Upload cancelled"))
                }
            } else if (rawError == null && uploadPrefs.autoResumesUsed(batchId) < MAX_AUTO_RESUMES) {
                delay(uploadPrefs.autoResumesUsed(batchId) * AUTO_RESUME_DELAY_MS)
                uploadPrefs.recordAutoResume(batchId)
                updateState { copy(statusLine = "Resuming…") }
                val resumed = UploadWorkScheduler.resumeFailed(appContext)
                if (resumed != null) {
                    updateState { copy(batchId = resumed) }
                    observeBatch(resumed)
                    return
                }
                updateState {
                    copy(
                        stage = UploadStage.ERROR,
                        statusLine = "Upload interrupted — try again",
                        failureLines = failedNames.map { "· $it" },
                    )
                }
            } else {
                val displayError = if (loginRequired) {
                    "$error — please log in again"
                } else {
                    error
                }
                updateState {
                    copy(
                        stage = UploadStage.ERROR,
                        statusLine = displayError,
                        failureLines = failedNames.map { "· $it" },
                    )
                }
                if (uploadPrefs.consumeErrorNotified(batchId)) {
                    UploadNotifications.showError(appContext, batchId, displayError, loginRequired)
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
                        applyAutoAccept(items, generatedFile)
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

    private suspend fun applyAutoAccept(items: List<StudyItemDto>, generatedFile: String) {
        val mode = settingsPrefs.autoAcceptMode.first()
        val autoDiscards = if (mode == AutoAcceptMode.EXTRACTED) {
            items.filter { it.origin != "EXTRACTED" }.map { it.id }.toSet()
        } else {
            emptySet()
        }
        updateState {
            copy(
                stage = UploadStage.DONE,
                statusLine = "${items.size} generated — discard anything off-topic",
                completedItems = totalItems,
                generatedCount = items.size,
                generatedItems = items,
                generatedFilePath = generatedFile,
                discardedIds = autoDiscards,
            )
        }
        if (mode == AutoAcceptMode.OFF || autoAcceptedPath == generatedFile) {
            return
        }
        autoAcceptedPath = generatedFile
        persistSelection(keepAll = mode == AutoAcceptMode.ALL)
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
                        val submitted = currentState.submittedPaths
                        uploadPrefs.setActiveBatchId(null)
                        uploadPrefs.clearFailed()
                        uploadPrefs.clearAutoResume()
                        launch {
                            deleteQuietly(submitted)
                        }
                        updateState {
                            copy(
                                curationBusy = false,
                                statusLine = "",
                                images = emptyList(),
                                files = emptyList(),
                                stagedImagePaths = emptyMap(),
                                generatedItems = emptyList(),
                                discardedIds = emptySet(),
                                generatedFilePath = null,
                                batchId = null,
                                submittedPaths = emptyList(),
                                lastSavedCount = kept,
                                stage = UploadStage.IDLE,
                            )
                        }
                        sendEffect(UploadEffect.NavigateToSuccess)
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

    private companion object {
        const val MAX_IMAGES = 5
        const val MAX_FILES = 10
        const val STAGED_DIR = "upload_staged"
        const val MAX_AUTO_RESUMES = 2
        const val AUTO_RESUME_DELAY_MS = 30_000L
    }
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
