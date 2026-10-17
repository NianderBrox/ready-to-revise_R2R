package com.r2r.readytorevise.presentation.dashboard
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.presentation.upload.UploadEvent
import com.r2r.readytorevise.presentation.upload.UploadStage
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import com.r2r.readytorevise.presentation.upload.UploadedFile
import com.r2r.readytorevise.presentation.upload.FileType
import com.r2r.readytorevise.presentation.upload.guessMime
import com.r2r.readytorevise.presentation.upload.resolveFileName
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.r2r.readytorevise.navigation.Screen
import com.r2r.readytorevise.ui.components.dashboard.DashboardButtons
import com.r2r.readytorevise.ui.components.dashboard.DashboardHeader
import com.r2r.readytorevise.ui.components.dashboard.StatTileRow
import com.r2r.readytorevise.ui.components.dashboard.StreakCard
import com.r2r.readytorevise.ui.components.common.DashboardSkeleton
import com.r2r.readytorevise.ui.components.dashboard.TopicsCard
import com.r2r.readytorevise.ui.theme.SkyBlue
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Description

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun DashboardRoute(
    appContainer: AppContainer,
    navController: NavHostController,
    uploadViewModel: UploadViewModel,
) {

    val factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                dashboardRepository = appContainer.dashboardRepository,
                recommendationsRepository = appContainer.recommendationsRepository,
                authRepository = appContainer.authRepository,
            )
        }
    }

    val viewModel: DashboardViewModel = viewModel(factory = factory)

    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                DashboardEffect.LoggedOut -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    DashboardScreen(
        state = state,
        onEvent = viewModel::onEvent,
        navController = navController,
        uploadViewModel = uploadViewModel,
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    navController: NavHostController,
    uploadViewModel: UploadViewModel
) {

    var showBottomSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = LocalActivity.current

    BackHandler {
        activity?.finish()
    }

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
                uploadViewModel.onEvent(UploadEvent.ClearImages)
                uploadViewModel.onEvent(UploadEvent.AddImage(cameraImageUri))

                showBottomSheet = false

                navController.navigate(Screen.Preview.route)
            }

        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                cameraLauncher.launch(cameraImageUri)
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                uploadViewModel.onEvent(UploadEvent.ClearImages)
                uploadViewModel.onEvent(UploadEvent.AddImage(uri))

                showBottomSheet = false

                navController.navigate(Screen.Preview.route)

            }

        }


    val pdfLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isNotEmpty()) {

                showBottomSheet = false

                val picked = uris.map { uri ->
                    val fileName = uri.resolveFileName(context)
                    UploadedFile(
                        uri = uri,
                        fileName = fileName,
                        type = FileType.PDF,
                        mimeType = guessMime(fileName),
                    )
                }
                uploadViewModel.onEvent(UploadEvent.SetFiles(picked))

                navController.navigate(Screen.DocumentPreview.route)

            }

        }
    val wordLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isNotEmpty()) {

                showBottomSheet = false

                val picked = uris.map { uri ->
                    val fileName = uri.resolveFileName(context)
                    UploadedFile(
                        uri = uri,
                        fileName = fileName,
                        type = FileType.WORD,
                        mimeType = guessMime(fileName),
                    )
                }
                uploadViewModel.onEvent(UploadEvent.SetFiles(picked))

                navController.navigate(Screen.DocumentPreview.route)

            }

        }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val uploadState by uploadViewModel.state.collectAsState()

    Scaffold(

        containerColor = MaterialTheme.colorScheme.background,

        topBar = {

            TopAppBar(

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlue
                ),

                title = {

                    DashboardHeader(

                        userName = state.userName.ifBlank { "there" },

                        onAnalyticsClick = {
                            navController.navigate(Screen.Analytics.route)
                        },

                        onProfileClick = {
                            navController.navigate(Screen.Profile.route)
                        },

                        onChangePasswordClick = {
                            navController.navigate(Screen.ChangePassword.route)
                        },

                        onNotificationSettingsClick = {
                            navController.navigate(Screen.NotificationSettings.route)
                        },

                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        },

                        onLogoutClick = {
                            onEvent(DashboardEvent.LogoutClicked)
                        }

                    )

                }

            )

        }

    ) { padding ->

        if (state.loading) {

            DashboardSkeleton(
                modifier = Modifier.padding(padding)
            )

            return@Scaffold

        }

        val loadError = state.error

        if (loadError != null && state.userName.isBlank()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(loadError)

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = { onEvent(DashboardEvent.RetryLoad) }) {
                    Text("Retry")
                }
            }

            return@Scaffold

        }

        Column(

            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),

            verticalArrangement = Arrangement.spacedBy(20.dp)

        ) {

            Spacer(modifier = Modifier.height(8.dp))

            StreakCard(
                streakDays = state.streakDays
            )

            StatTileRow(
                dueToday = state.dueToday,
                completedToday = state.completedToday,
                upcoming = state.upcoming
            )

            TopicsCard(
                readyCount = state.readyToReviseCount,
                topTitle = state.topRecommendationTitle,
                onRevisionClick = {
                    navController.navigate(Screen.Revision.route)
                }
            )

            DashboardButtons(

                onAddClick = {
                    showBottomSheet = true
                },

                onRevisionClick = {
                    navController.navigate(Screen.Revision.route)
                },

                revisionEnabled = state.readyToReviseCount > 0
            )

            Spacer(modifier = Modifier.height(30.dp))

        }

        val uploadOverlayVisible = uploadState.stage == UploadStage.WORKING ||
            (uploadState.stage == UploadStage.DONE && uploadState.batchId != null) ||
            uploadState.stage == UploadStage.ERROR

        if (uploadOverlayVisible) {
            val overlayProgress: Float? = when {
                uploadState.stage == UploadStage.ERROR -> 1f
                uploadState.curationBusy -> null
                uploadState.stage == UploadStage.DONE -> 1f
                uploadState.totalItems > 0 ->
                    uploadState.completedItems.toFloat() / uploadState.totalItems.toFloat()
                else -> null
            }

            val overlayMessage: String = when {
                uploadState.stage == UploadStage.ERROR ->
                    uploadState.statusLine.ifBlank { "Upload failed" }
                uploadState.curationBusy -> "Saving…"
                uploadState.stage == UploadStage.DONE -> "Tap to review questions"
                else -> uploadState.statusLine.ifBlank { "Working…" }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                UploadProgressOverlay(
                    progress = overlayProgress,
                    message = overlayMessage,
                    isError = uploadState.stage == UploadStage.ERROR,
                    showCancel = uploadState.stage == UploadStage.WORKING,
                    onOpen = { navController.navigate(Screen.Processing.route) },
                    onCancel = { uploadViewModel.onEvent(UploadEvent.CancelActiveBatch) },
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp
                    )
                )
            }
        }

    }

    if (showBottomSheet) {

        ModalBottomSheet(

            onDismissRequest = {
                showBottomSheet = false
            },

            sheetState = sheetState

        ) {

            Surface(

                onClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraLauncher.launch(cameraImageUri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
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

            Surface(

                onClick = {

                    wordLauncher.launch(
                        arrayOf(
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
