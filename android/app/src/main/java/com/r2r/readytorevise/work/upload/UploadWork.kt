package com.r2r.readytorevise.work.upload

import androidx.work.Data
import androidx.work.workDataOf
import java.util.UUID

object UploadWork {
    const val NOTIFICATION_ID = 2001
    const val STATUS_READY = "READY"
    const val STATUS_FAILED = "FAILED"

    const val EXTRA_OPEN_PROCESSING = "com.r2r.readytorevise.OPEN_PROCESSING"
    const val ACTION_CANCEL_BATCH = "com.r2r.readytorevise.UPLOAD_CANCEL_BATCH"
    const val ACTION_RETRY_BATCH = "com.r2r.readytorevise.UPLOAD_RETRY_BATCH"
    const val EXTRA_BATCH_ID = "batch_id"

    const val IN_URIS = "in_uris"
    const val IN_NAMES = "in_names"
    const val IN_MIMES = "in_mimes"
    const val IN_PATHS = "in_paths"
    const val IN_DOC_IDS = "in_doc_ids"
    const val IN_DISCARD_IDS = "in_discard_ids"
    const val IN_GENERATED_FILE = "in_generated_file"
    const val IN_BATCH_ID = "in_batch_id"
    const val IN_TOTAL = "in_total"

    const val P_STAGE = "p_stage"
    const val P_COMPLETED = "p_completed"
    const val P_TOTAL = "p_total"
    const val P_MESSAGE = "p_message"

    const val OUT_PATHS = "out_paths"
    const val OUT_NAMES = "out_names"
    const val OUT_MIMES = "out_mimes"
    const val OUT_DOC_IDS = "out_doc_ids"
    const val OUT_GENERATED_FILE = "out_generated_file"
    const val OUT_KEPT = "out_kept"
    const val OUT_ERROR = "out_error"
    const val OUT_FAILED_FILES = "out_failed_files"
    const val OUT_CANCELLED = "out_cancelled"
    const val OUT_LOGIN_REQUIRED = "out_login_required"

    const val STAGE_PREPARE = "prepare"
    const val STAGE_UPLOAD = "upload"
    const val STAGE_ANALYZE = "analyze"
    const val STAGE_GENERATE = "generate"
    const val STAGE_SAVE = "save"

    fun batchName(batchId: String) = "upload_batch_$batchId"

    fun persistName(batchId: String) = "upload_persist_$batchId"

    fun newBatchId(): String = UUID.randomUUID().toString()

    fun batchDirName(batchId: String) = "upload_$batchId"

    fun forward(input: Data, vararg extra: Pair<String, Any?>): Data {
        val pairs = mutableListOf<Pair<String, Any?>>()
        input.keyValueMap.forEach { (key, value) ->
            if (key in PASSTHROUGH_KEYS) {
                pairs += key to value
            }
        }
        pairs.addAll(extra)
        return workDataOf(*pairs.toTypedArray())
    }

    private val PASSTHROUGH_KEYS = setOf(
        IN_BATCH_ID,
        IN_TOTAL,
        IN_URIS,
        IN_NAMES,
        IN_MIMES,
        IN_PATHS,
        IN_DOC_IDS,
    )

    data class BatchItem(val uri: String, val name: String, val mime: String)
}
