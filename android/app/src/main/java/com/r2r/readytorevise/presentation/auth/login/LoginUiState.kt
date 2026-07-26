package com.r2r.readytorevise.presentation.auth.login

import com.r2r.readytorevise.presentation.base.UiState

data class LoginUiState(
    val email: String = "",
    val password: String = "",

    val isLoading: Boolean = false,
    val isLoginEnabled: Boolean = false,

    val emailError: String? = null,

    val showPasswordError: Boolean = false,
    val passwordError: String = ""
) : UiState