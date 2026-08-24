package com.r2r.readytorevise.presentation.quiz

import com.r2r.readytorevise.presentation.base.UiEffect
import com.r2r.readytorevise.presentation.base.UiEvent

sealed interface RevisionEvent : UiEvent {
    data object RetryLoad : RevisionEvent
    data class SelectOption(val index: Int) : RevisionEvent
    data object NextClicked : RevisionEvent
    data object BackClicked : RevisionEvent
    data object MemorizeSwiped : RevisionEvent
    data object FinishConfirmed : RevisionEvent
    data object ExitConfirmed : RevisionEvent
}

sealed interface RevisionEffect : UiEffect {
    data object NavigateBack : RevisionEffect
}
