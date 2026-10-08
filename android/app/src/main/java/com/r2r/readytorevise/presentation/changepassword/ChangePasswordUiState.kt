package com.r2r.readytorevise.presentation.changepassword

import com.r2r.readytorevise.presentation.base.UiState

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val canSubmit: Boolean = false,
) : UiState
