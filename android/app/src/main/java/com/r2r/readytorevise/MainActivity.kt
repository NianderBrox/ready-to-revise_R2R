package com.r2r.readytorevise

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.r2r.readytorevise.ui.theme.ReadyToReviseTheme
import com.r2r.readytorevise.navigation.AppNavGraph
import com.r2r.readytorevise.notification.NotificationHelper

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        val appContainer = (application as ReadyToReviseApplication).container
        setContent {
            ReadyToReviseTheme {
                AppNavGraph(appContainer = appContainer)
            }
        }

        // TODO: Remove this test block after testing is done
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            NotificationHelper.sendRevisionDueNotification(this, 5)
        }, 2000)
        handler.postDelayed({
            NotificationHelper.sendNoRevisionNotification(this)
        }, 4000)
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