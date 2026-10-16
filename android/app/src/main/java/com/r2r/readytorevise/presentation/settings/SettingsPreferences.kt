package com.r2r.readytorevise.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

enum class AutoAcceptMode(val label: String, val description: String) {
    ALL(
        "All",
        "Every generated question is accepted automatically — no review step."
    ),
    EXTRACTED(
        "Extracted",
        "Questions taken straight from your notes are accepted automatically — AI-generated ones are left out."
    ),
    OFF(
        "Off",
        "Review every question before it is added to your deck."
    );

    companion object {
        fun fromName(name: String?): AutoAcceptMode {
            return entries.firstOrNull { it.name == name } ?: OFF
        }
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }
}

class SettingsPreferences(private val context: Context) {

    companion object {
        private val AUTO_ACCEPT_MODE = stringPreferencesKey("auto_accept_mode")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val autoAcceptMode: Flow<AutoAcceptMode> = context.settingsDataStore.data.map { prefs ->
        AutoAcceptMode.fromName(prefs[AUTO_ACCEPT_MODE])
    }

    suspend fun setAutoAcceptMode(mode: AutoAcceptMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_ACCEPT_MODE] = mode.name
        }
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.fromName(prefs[THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }
}
