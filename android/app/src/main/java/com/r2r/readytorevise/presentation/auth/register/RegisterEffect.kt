package com.r2r.readytorevise.presentation.auth.register

import com.r2r.readytorevise.presentation.base.UiEffect

sealed class RegisterEffect : UiEffect {
    data object NavigateToLogin : RegisterEffect()
    data class ShowSnackbar(val message: String) : RegisterEffect()
}
