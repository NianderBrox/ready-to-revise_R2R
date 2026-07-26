package com.r2r.readytorevise.presentation.auth.register

import com.r2r.readytorevise.presentation.base.UiState

data class RegisterUiState(

    val name: String = "",

    val email: String = "",

    val password: String = "",

    val confirmPassword: String = "",

    val isLoading: Boolean = false,

    val isRegisterEnabled: Boolean = false

) : UiState