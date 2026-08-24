package com.r2r.readytorevise.presentation.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                val picked = uris.map { uri ->
                    val fileName = uri.resolveFileName(context)
                    val type = when {
                        fileName.endsWith(".pdf", true) -> FileType.PDF
                        else -> FileType.WORD
                    }
                    UploadedFile(
                        uri = uri,
                        fileName = fileName,
                        type = type,
                        mimeType = guessMime(fileName),
                    )
                }
                uploadViewModel.onEvent(UploadEvent.AddFiles(picked))
            }
        }

    val state by uploadViewModel.state.collectAsState()

    DocumentPreviewScreen(
        navController = navController,
        uploadedFiles = state.files,
        onAddDocument = {
            addDocumentLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            )
        },
        onRemoveDocument = {
            uploadViewModel.onEvent(UploadEvent.RemoveFile(it))
        },
        onSubmit = {
            uploadViewModel.onEvent(UploadEvent.Submit)
            navController.navigate("processing")
        }
    )
}
