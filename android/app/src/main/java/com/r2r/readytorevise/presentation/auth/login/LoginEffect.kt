package com.r2r.readytorevise.presentation.auth.login



import com.r2r.readytorevise.presentation.base.UiEffect
sealed interface LoginEffect : UiEffect {

    data object NavigateToDashboard : LoginEffect

    data object NavigateToRegister : LoginEffect

    data class ShowError(
        val message: String
    ) : LoginEffect
}