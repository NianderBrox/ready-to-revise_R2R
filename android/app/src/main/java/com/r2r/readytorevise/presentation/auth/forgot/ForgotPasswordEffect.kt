package com.r2r.readytorevise.presentation.auth.forgot

import com.r2r.readytorevise.presentation.base.UiEffect

sealed interface ForgotPasswordEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ForgotPasswordEffect
    data class NavigateToLogin(val message: String = "") : ForgotPasswordEffect
}
