package com.r2r.readytorevise.presentation.upload

import android.net.Uri
import com.r2r.readytorevise.presentation.base.UiEffect
import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface UploadEvent : UiEvent {
    data class AddImage(val uri: Uri) : UploadEvent
    data class ReplaceImage(val index: Int, val uri: Uri) : UploadEvent
    data class RemoveImage(val index: Int) : UploadEvent
    data object ClearImages : UploadEvent
    data class AddFiles(val files: List<UploadedFile>) : UploadEvent
    data class SetFiles(val files: List<UploadedFile>) : UploadEvent
    data class RemoveFile(val index: Int) : UploadEvent
    data object Submit : UploadEvent
    data class ToggleDiscard(val studyItemId: String) : UploadEvent
    data object ConfirmCuration : UploadEvent
    data object SkipCuration : UploadEvent
}

sealed interface UploadEffect : UiEffect {
    data object NavigateToDashboard : UploadEffect
    data class ShowToast(val message: String) : UploadEffect
}
