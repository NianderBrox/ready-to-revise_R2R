package com.r2r.readytorevise.presentation.upload

import android.net.Uri

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