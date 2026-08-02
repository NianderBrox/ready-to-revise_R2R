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

}