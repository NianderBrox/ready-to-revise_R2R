package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.r2r.readytorevise.data.local.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FailedBatch(
    val batchId: String,
    val uris: List<String> = emptyList(),
    val paths: List<String> = emptyList(),
    val names: List<String> = emptyList(),
    val mimes: List<String> = emptyList(),
    val docIds: List<String> = emptyList(),
)

class UploadPrefs(context: Context) {
    private val store = context.dataStore

    val activeBatchId: Flow<String?> = store.data.map { it[ACTIVE_BATCH] }

    suspend fun setActiveBatchId(batchId: String?) {
        store.edit { prefs ->
            if (batchId == null) {
                prefs.remove(ACTIVE_BATCH)
            } else {
                prefs[ACTIVE_BATCH] = batchId
            }
        }
    }

    suspend fun saveFailed(batch: FailedBatch) {
        store.edit { prefs ->
            prefs[FAILED_BATCH] = json.encodeToString(batch)
        }
    }

    suspend fun readFailed(): FailedBatch? {
        val raw = store.data.map { it[FAILED_BATCH] }.first() ?: return null
        return try {
            json.decodeFromString(raw)
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun clearFailed() {
        store.edit { prefs ->
            prefs.remove(FAILED_BATCH)
        }
    }

    private companion object {
        val ACTIVE_BATCH = stringPreferencesKey("upload_active_batch")
        val FAILED_BATCH = stringPreferencesKey("upload_failed_batch")
        val json = Json { ignoreUnknownKeys = true }
    }
}
