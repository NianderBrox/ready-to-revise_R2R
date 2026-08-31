package com.r2r.readytorevise.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

class NotificationPreferences(private val context: Context) {

    companion object {
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    }

    val notificationsEnabled: Flow<Boolean> = context.notificationDataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED] ?: true
    }

    val notificationHour: Flow<Int> = context.notificationDataStore.data.map { prefs ->
        prefs[NOTIFICATION_HOUR] ?: 10
    }

    val notificationMinute: Flow<Int> = context.notificationDataStore.data.map { prefs ->
        prefs[NOTIFICATION_MINUTE] ?: 0
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.notificationDataStore.edit { prefs ->
            prefs[NOTIFICATION_HOUR] = hour
            prefs[NOTIFICATION_MINUTE] = minute
        }
    }
}
