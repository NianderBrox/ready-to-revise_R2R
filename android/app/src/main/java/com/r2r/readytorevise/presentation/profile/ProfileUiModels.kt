package com.r2r.readytorevise.presentation.profile

import com.r2r.readytorevise.presentation.base.UiEffect
import com.r2r.readytorevise.presentation.base.UiEvent
import com.r2r.readytorevise.presentation.base.UiState

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
) : UiState

sealed interface ProfileEvent : UiEvent {
    data object RetryLoad : ProfileEvent
    data object LogoutClicked : ProfileEvent
}

sealed interface ProfileEffect : UiEffect {
    data object LoggedOut : ProfileEffect
}
