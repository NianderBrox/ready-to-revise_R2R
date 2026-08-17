package com.r2r.readytorevise.presentation.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
@Composable
fun UploadScreen(
    navController: NavController,
    uploadViewModel: UploadViewModel
) {

    val replaceIndex = remember {
        androidx.compose.runtime.mutableIntStateOf(-1)
    }


    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                if (replaceIndex.intValue == -1) {

                    uploadViewModel.addImage(uri)

                } else {

                    uploadViewModel.replaceImage(
                        replaceIndex.intValue,
                        uri
                    )

                    replaceIndex.intValue = -1

                }

            }

        }


    PreviewScreen(

        navController = navController,

        images = uploadViewModel.images,


        onAddImage = {

            galleryLauncher.launch(

                PickVisualMediaRequest(

                    ActivityResultContracts
                        .PickVisualMedia
                        .ImageOnly

                )

            )

        },


        onReplaceImage = { index ->

            replaceIndex.intValue = index


            galleryLauncher.launch(

                PickVisualMediaRequest(

                    ActivityResultContracts
                        .PickVisualMedia
                        .ImageOnly

                )

            )

        },


        onRemoveImage = { index ->

            uploadViewModel.removeImage(index)

        },


        onSubmit = {

            navController.navigate("processing")

        }

    )

}