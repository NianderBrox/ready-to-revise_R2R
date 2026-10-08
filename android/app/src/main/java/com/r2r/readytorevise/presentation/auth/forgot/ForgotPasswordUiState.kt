package com.r2r.readytorevise.presentation.auth.forgot

import com.r2r.readytorevise.presentation.base.UiState

enum class ForgotStep { EMAIL, CODE }

data class ForgotPasswordUiState(
    val step: ForgotStep = ForgotStep.EMAIL,
    val email: String = "",
    val emailError: String? = null,
    val otp: String = "",
    val otpError: String? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val canSendCode: Boolean = false,
    val canReset: Boolean = false,
    val resendCooldownSeconds: Int = 0,
) : UiState
