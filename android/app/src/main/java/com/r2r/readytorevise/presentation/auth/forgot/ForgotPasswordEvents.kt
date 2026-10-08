package com.r2r.readytorevise.presentation.auth.forgot

import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface ForgotPasswordEvent : UiEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent
    data class OtpChanged(val otp: String) : ForgotPasswordEvent
    data class NewPasswordChanged(val password: String) : ForgotPasswordEvent
    data class ConfirmPasswordChanged(val password: String) : ForgotPasswordEvent
    data object SendCodeClicked : ForgotPasswordEvent
    data object ResendCodeClicked : ForgotPasswordEvent
    data object ResetClicked : ForgotPasswordEvent
    data object ChangeEmailClicked : ForgotPasswordEvent
}
