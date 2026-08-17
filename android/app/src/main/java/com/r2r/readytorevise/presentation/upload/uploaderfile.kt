package com.r2r.readytorevise.presentation.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class UploadedFile(
    val uri: Uri,
    val fileName: String,
    val type: FileType
)

enum class FileType {
    IMAGE,
    PDF,
    WORD
}

fun Uri.resolveFileName(context: Context): String {
    var name = "document"
    context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}