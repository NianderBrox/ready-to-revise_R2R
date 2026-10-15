package com.r2r.readytorevise.work.upload

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val generatedDocIds: List<String> = emptyList(),
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

    val batteryPromptShown: Flow<Boolean> = store.data.map { it[BATTERY_PROMPT] == true }

    suspend fun setBatteryPromptShown() {
        store.edit { prefs ->
            prefs[BATTERY_PROMPT] = true
        }
    }

    suspend fun autoResumesUsed(batchId: String): Int {
        val current = store.data.map { it[AUTO_BATCH] }.first()
        if (current != batchId) {
            return 0
        }
        return store.data.map { it[AUTO_COUNT] ?: 0 }.first()
    }

    suspend fun recordAutoResume(batchId: String) {
        val current = store.data.map { it[AUTO_BATCH] }.first()
        store.edit { prefs ->
            if (current != batchId) {
                prefs[AUTO_BATCH] = batchId
                prefs[AUTO_COUNT] = 1
            } else {
                prefs[AUTO_COUNT] = (prefs[AUTO_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun clearAutoResume() {
        store.edit { prefs ->
            prefs.remove(AUTO_BATCH)
            prefs.remove(AUTO_COUNT)
        }
    }

    suspend fun consumeErrorNotified(batchId: String): Boolean {
        val current = store.data.map { it[NOTIFIED_ERROR] }.first()
        if (current == batchId) {
            return false
        }
        store.edit { prefs ->
            prefs[NOTIFIED_ERROR] = batchId
        }
        return true
    }

    val completedBatches: Flow<Set<String>> = store.data.map { it[COMPLETED] ?: emptySet() }

    suspend fun markBatchCompleted(batchId: String) {
        store.edit { prefs ->
            prefs[COMPLETED] = (prefs[COMPLETED] ?: emptySet()) + batchId
        }
    }

    suspend fun isBatchCompleted(batchId: String): Boolean {
        return store.data.map { it[COMPLETED]?.contains(batchId) == true }.first()
    }

    private companion object {
        val ACTIVE_BATCH = stringPreferencesKey("upload_active_batch")
        val FAILED_BATCH = stringPreferencesKey("upload_failed_batch")
        val BATTERY_PROMPT = booleanPreferencesKey("upload_battery_prompt")
        val AUTO_BATCH = stringPreferencesKey("upload_auto_batch")
        val AUTO_COUNT = intPreferencesKey("upload_auto_count")
        val NOTIFIED_ERROR = stringPreferencesKey("upload_notified_error")
        val COMPLETED = stringSetPreferencesKey("upload_completed_batches")
        val json = Json { ignoreUnknownKeys = true }
    }
}
