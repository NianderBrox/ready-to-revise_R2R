package com.r2r.readytorevise.presentation.upload

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun DocumentUploadScreen(

    navController: NavController,

    uploadViewModel: UploadViewModel

) {

    DocumentPreviewScreen(

        navController = navController,

        uploadedFiles = uploadViewModel.uploadedFiles,

        onAddDocument = { },

        onRemoveDocument = {

            uploadViewModel.removeFile(it)

        },

        onSubmit = {

            navController.navigate("processing")

        }

    )

}