package com.r2r.readytorevise.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsState(
    val notificationsEnabled: Boolean = true,
    val hour: Int = 10,
    val minute: Int = 0,
    val isLoading: Boolean = true
)

sealed interface NotificationSettingsEvent {
    data class SetEnabled(val enabled: Boolean) : NotificationSettingsEvent
    data class SetTime(val hour: Int, val minute: Int) : NotificationSettingsEvent
}

class NotificationSettingsViewModel(
    private val preferences: NotificationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.notificationsEnabled.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled, isLoading = false) }
            }
        }
        viewModelScope.launch {
            preferences.notificationHour.collect { hour ->
                _state.update { it.copy(hour = hour) }
            }
        }
        viewModelScope.launch {
            preferences.notificationMinute.collect { minute ->
                _state.update { it.copy(minute = minute) }
            }
        }
    }

    fun onEvent(event: NotificationSettingsEvent) {
        when (event) {
            is NotificationSettingsEvent.SetEnabled -> {
                viewModelScope.launch {
                    preferences.setNotificationsEnabled(event.enabled)
                }
            }
            is NotificationSettingsEvent.SetTime -> {
                viewModelScope.launch {
                    preferences.setNotificationTime(event.hour, event.minute)
                }
            }
        }
    }
}
