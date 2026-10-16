package com.r2r.readytorevise

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.r2r.readytorevise.presentation.settings.SettingsPreferences
import com.r2r.readytorevise.presentation.settings.ThemeMode
import com.r2r.readytorevise.ui.theme.ReadyToReviseTheme
import com.r2r.readytorevise.navigation.AppNavGraph
import com.r2r.readytorevise.work.upload.UploadWork

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        val appContainer = (application as ReadyToReviseApplication).container
        val openProcessing =
            intent?.getBooleanExtra(UploadWork.EXTRA_OPEN_PROCESSING, false) == true
        val processingBatchId =
            intent?.getStringExtra(UploadWork.EXTRA_BATCH_ID)
        setContent {
            val context = LocalContext.current
            val settingsPrefs = remember { SettingsPreferences(context.applicationContext) }
            val themeMode by settingsPrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            ReadyToReviseTheme(darkTheme = darkTheme) {
                AppNavGraph(
                    appContainer = appContainer,
                    openProcessing = openProcessing,
                    processingBatchId = processingBatchId,
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}