package com.r2r.readytorevise.presentation.upload

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class UploadViewModel : ViewModel() {

    val images = mutableStateListOf<Uri>()

    fun addImage(uri: Uri) {

        if (images.size < 5) {

            images.add(uri)

        }

    }

    fun replaceImage(

        index: Int,

        uri: Uri

    ) {

        if (index in images.indices) {

            images[index] = uri

        }

    }

    fun removeImage(index: Int) {

        if (index in images.indices) {

            images.removeAt(index)

        }

    }

    fun clearImages() {

        images.clear()

    }

    val uploadedFiles = mutableStateListOf<UploadedFile>()

    fun addFile(file: UploadedFile) {

        uploadedFiles.add(file)

    }

    fun replaceFile(

        index: Int,

        file: UploadedFile

    ) {

        if (index in uploadedFiles.indices) {

            uploadedFiles[index] = file

        }

    }

    fun removeFile(index: Int) {

        if (index in uploadedFiles.indices) {

            uploadedFiles.removeAt(index)

        }

    }

    fun clearFiles() {

        uploadedFiles.clear()

    }

}