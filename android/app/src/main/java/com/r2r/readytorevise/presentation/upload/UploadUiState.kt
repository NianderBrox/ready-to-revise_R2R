package com.r2r.readytorevise.presentation.upload

import android.net.Uri
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.presentation.base.UiState

enum class UploadStage {
    IDLE,
    WORKING,
    DONE,
    ERROR,
}

data class UploadUiState(
    val images: List<Uri> = emptyList(),
    val files: List<UploadedFile> = emptyList(),
    val stage: UploadStage = UploadStage.IDLE,
    val statusLine: String = "",
    val completedItems: Int = 0,
    val totalItems: Int = 0,
    val generatedCount: Int = 0,
    val failureLines: List<String> = emptyList(),
    val errorCountdownSeconds: Int? = null,
    val generatedItems: List<StudyItemDto> = emptyList(),
    val discardedIds: Set<String> = emptySet(),
    val curationBusy: Boolean = false,
) : UiState {
    val isBusy: Boolean
        get() = stage == UploadStage.WORKING

    val canSubmit: Boolean
        get() = !isBusy && (images.isNotEmpty() || files.isNotEmpty())

    val keptCount: Int
        get() = generatedItems.size - discardedIds.size
}
