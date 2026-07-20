package com.r2r.readytorevise.presentation.auth.login

import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface LoginEvent : UiEvent {

    data class EmailChanged(
        val email: String
    ) : LoginEvent

    data class PasswordChanged(
        val password: String
    ) : LoginEvent

    data object LoginClicked : LoginEvent

    data object RegisterClicked : LoginEvent
}