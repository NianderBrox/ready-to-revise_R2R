package com.r2r.readytorevise.presentation.auth.register

import com.r2r.readytorevise.presentation.base.UiEffect

sealed class RegisterEffect : UiEffect {
    data class NavigateToLogin(val message: String = "") : RegisterEffect()
    data class ShowSnackbar(val message: String) : RegisterEffect()
}
