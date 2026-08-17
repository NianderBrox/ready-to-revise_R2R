package com.r2r.readytorevise.presentation.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
abstract class BaseViewModel<
        STATE : UiState,
        EVENT : UiEvent,
        EFFECT : UiEffect
        >(
    initialState: STATE
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = _state.asStateFlow()

    protected val currentState: STATE
        get() = _state.value

    private val _effect = MutableSharedFlow<EFFECT>()
    val effect: SharedFlow<EFFECT> = _effect.asSharedFlow()



    protected fun updateState(
        reducer: STATE.() -> STATE
    ) {
        _state.update(reducer)
    }

    protected suspend fun sendEffect(
        effect: EFFECT
    ) {
        _effect.emit(effect)
    }

    abstract fun onEvent(event: EVENT)
}