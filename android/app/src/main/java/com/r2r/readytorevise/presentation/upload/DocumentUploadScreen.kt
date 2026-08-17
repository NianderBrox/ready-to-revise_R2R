package com.r2r.readytorevise.presentation.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Composable
fun DocumentUploadScreen(
    navController: NavController,
    uploadViewModel: UploadViewModel
) {
    val context = LocalContext.current

    val addDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    val fileName = uri.resolveFileName(context)
                    val type = when {
                        fileName.endsWith(".pdf", true) -> FileType.PDF
                        else -> FileType.WORD
                    }
                    uploadViewModel.addFile(
                        UploadedFile(
                            uri = uri,
                            fileName = fileName,
                            type = type
                        )
                    )
                }
            }
        }

    DocumentPreviewScreen(
        navController = navController,
        uploadedFiles = uploadViewModel.uploadedFiles,
        onAddDocument = {
            addDocumentLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            )
        },
        onRemoveDocument = {
            uploadViewModel.removeFile(it)
        },
        onSubmit = {
            navController.navigate("processing")
        }
    )
}
