package com.r2r.readytorevise.presentation.changepassword

import com.r2r.readytorevise.presentation.base.UiEffect

sealed interface ChangePasswordEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ChangePasswordEffect
    data class Finished(val message: String) : ChangePasswordEffect
}
