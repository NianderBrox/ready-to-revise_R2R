package com.r2r.readytorevise.presentation.dashboard
import androidx.lifecycle.viewmodel.compose.viewModel
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.r2r.readytorevise.navigation.Screen
import com.r2r.readytorevise.ui.components.dashboard.DashboardButtons
import com.r2r.readytorevise.ui.components.dashboard.DashboardHeader
import com.r2r.readytorevise.ui.components.dashboard.StreakCard
import com.r2r.readytorevise.ui.components.dashboard.TopicsCard
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Description
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController
) {

    var showBottomSheet by remember { mutableStateOf(false) }
    val uploadViewModel: UploadViewModel = viewModel()
    val context = LocalContext.current

    val cameraImageUri = remember {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "R2R_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/ReadyToRevise"
                )
            }
        }

        context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    }
    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                uploadViewModel.clearImages()
                uploadViewModel.addImage(cameraImageUri)

                showBottomSheet = false

                navController.navigate(Screen.Preview.route)
            }

        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                uploadViewModel.clearImages()
                uploadViewModel.addImage(uri)

                showBottomSheet = false

                navController.navigate(Screen.Preview.route)

            }

        }


    val pdfLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {

                showBottomSheet = false
                navController.navigate(Screen.Processing.route)  // We'll handle this later
            }
        }

    val wordLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {

                showBottomSheet = false
                navController.navigate(Screen.Processing.route)  // We'll handle this later
            }
        }


    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Scaffold(

        containerColor = Color(0xFF23336F),

        topBar = {

            TopAppBar(

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF23336F)
                ),

                title = {

                    DashboardHeader(

                        userName = "Vaibhav Singh",

                        onProfileClick = {
                            navController.navigate(Screen.Profile.route)
                        },

                        onChangePasswordClick = {

                        },

                        onLogoutClick = {
                            navController.navigate(Screen.Login.route)
                        }

                    )

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF23336F))
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),

            verticalArrangement = Arrangement.spacedBy(20.dp)

        ) {

            Spacer(modifier = Modifier.height(8.dp))

            StreakCard()

            TopicsCard()

            DashboardButtons(

                onAddClick = {
                    showBottomSheet = true
                },

                onRevisionClick = {
                    navController.navigate(Screen.Revision.route)
                }

            )

            Spacer(modifier = Modifier.height(30.dp))

        }

    }

    if (showBottomSheet) {

        ModalBottomSheet(

            onDismissRequest = {
                showBottomSheet = false
            },

            sheetState = sheetState

        ) {

            // CAMERA
            Surface(

                onClick = {
                    cameraLauncher.launch(cameraImageUri)
                },

                color = Color.Transparent

            ) {

                ListItem(

                    headlineContent = {
                        Text("Take Photo")
                    },

                    supportingContent = {
                        Text("Capture notes using camera")
                    },

                    leadingContent = {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null
                        )
                    },

                    modifier = Modifier.fillMaxWidth()

                )

            }

            // GALLERY
            Surface(

                onClick = {

                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )

                },

                color = Color.Transparent

            ) {

                ListItem(

                    headlineContent = {
                        Text("Choose from Gallery")
                    },

                    supportingContent = {
                        Text("Select an existing image")
                    },

                    leadingContent = {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null
                        )
                    },

                    modifier = Modifier.fillMaxWidth()

                )

            }

            // PDF
            Surface(

                onClick = {

                    pdfLauncher.launch(
                        arrayOf("application/pdf")
                    )

                },

                color = Color.Transparent

            ) {

                ListItem(

                    headlineContent = {
                        Text("Upload PDF")
                    },

                    supportingContent = {
                        Text("Select a PDF document")
                    },

                    leadingContent = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null
                        )
                    },

                    modifier = Modifier.fillMaxWidth()

                )

            }

            // WORD
            Surface(

                onClick = {

                    wordLauncher.launch(
                        arrayOf(
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )

                },

                color = Color.Transparent

            ) {

                ListItem(

                    headlineContent = {
                        Text("Upload Word Document")
                    },

                    supportingContent = {
                        Text("Select a Word document")
                    },

                    leadingContent = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null
                        )
                    },

                    modifier = Modifier.fillMaxWidth()

                )

            }

            Spacer(modifier = Modifier.height(20.dp))

        }

    }
}
