package com.r2r.readytorevise.presentation.changepassword

import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface ChangePasswordEvent : UiEvent {
    data class CurrentPasswordChanged(val password: String) : ChangePasswordEvent
    data class NewPasswordChanged(val password: String) : ChangePasswordEvent
    data class ConfirmPasswordChanged(val password: String) : ChangePasswordEvent
    data object SubmitClicked : ChangePasswordEvent
}
