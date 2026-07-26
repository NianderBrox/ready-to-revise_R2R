package com.r2r.readytorevise.presentation.auth.register

import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface RegisterEvent : UiEvent {

    data class NameChanged(val name: String) : RegisterEvent

    data class EmailChanged(val email: String) : RegisterEvent

    data class PasswordChanged(val password: String) : RegisterEvent

    data class ConfirmPasswordChanged(val password: String) : RegisterEvent

    data object RegisterClicked : RegisterEvent

    data object LoginClicked : RegisterEvent
}