package com.r2r.readytorevise.presentation.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPrefs(private val context: Context) {

    val permissionsDone: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[DONE] == true
    }

    suspend fun setPermissionsDone() {
        context.onboardingDataStore.edit { prefs ->
            prefs[DONE] = true
        }
    }

    private companion object {
        val DONE = booleanPreferencesKey("permissions_done")
    }
}
