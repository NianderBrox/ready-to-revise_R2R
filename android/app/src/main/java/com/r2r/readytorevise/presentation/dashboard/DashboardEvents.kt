package com.r2r.readytorevise.presentation.dashboard

import com.r2r.readytorevise.presentation.base.UiEffect
import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface DashboardEvent : UiEvent {
    data object RetryLoad : DashboardEvent
    data object LogoutClicked : DashboardEvent
}

sealed interface DashboardEffect : UiEffect {
    data object LoggedOut : DashboardEffect
}
