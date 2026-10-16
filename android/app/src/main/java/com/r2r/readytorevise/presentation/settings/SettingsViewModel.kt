package com.r2r.readytorevise.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val autoAcceptMode: AutoAcceptMode = AutoAcceptMode.OFF,
    val isLoading: Boolean = true
)

sealed interface SettingsEvent {
    data class SetAutoAcceptMode(val mode: AutoAcceptMode) : SettingsEvent
}

class SettingsViewModel(
    private val preferences: SettingsPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.autoAcceptMode.collect { mode ->
                _state.update { it.copy(autoAcceptMode = mode, isLoading = false) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetAutoAcceptMode -> {
                viewModelScope.launch {
                    preferences.setAutoAcceptMode(event.mode)
                }
            }
        }
    }
}
