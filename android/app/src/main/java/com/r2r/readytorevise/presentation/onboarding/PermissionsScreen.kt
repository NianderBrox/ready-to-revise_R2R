package com.r2r.readytorevise.presentation.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.work.upload.UploadPrefs
import kotlinx.coroutines.launch

@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uploadPrefs = remember { UploadPrefs(context.applicationContext) }

    var resumeTick by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        resumeTick++
    }

    val tick = resumeTick
    val notificationsGranted = remember(tick) { hasNotifications(context) }
    val alarmsGranted = remember(tick) { canScheduleAlarms(context) }
    val batteryExempt = remember(tick) { isBatteryExempt(context) }
    val cameraGranted = remember(tick) { hasCamera(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Grant these once so uploads, reminders and the camera keep working.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(20.dp))

        PermissionRow(
            title = "Notifications",
            description = "Upload progress and revision reminders.",
            granted = notificationsGranted,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionRow(
            title = "Exact alarms",
            description = "On-time revision reminders.",
            granted = alarmsGranted,
            actionLabel = "Open settings",
            onGrant = {
                openAppSettingsPage(
                    context,
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionRow(
            title = "Background running",
            description = "Lets uploads finish when the app is closed.",
            granted = batteryExempt,
            actionLabel = "Open settings",
            onGrant = {
                scope.launch {
                    uploadPrefs.setBatteryPromptShown()
                }
                openAppSettingsPage(
                    context,
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionRow(
            title = "Camera",
            description = "Capture notes directly in the app.",
            granted = cameraGranted,
            onGrant = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        R2RButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
    actionLabel: String = "Allow",
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (granted) {
                Text(
                    text = "Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2E7D32),
                )
            } else {
                Button(onClick = onGrant) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun hasNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun canScheduleAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return true
    }
    val alarms = context.getSystemService(AlarmManager::class.java) ?: return false
    return alarms.canScheduleExactAlarms()
}

private fun isBatteryExempt(context: Context): Boolean {
    val power = context.getSystemService(PowerManager::class.java) ?: return false
    return power.isIgnoringBatteryOptimizations(context.packageName)
}

private fun hasCamera(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openAppSettingsPage(context: Context, action: String) {
    val intent = Intent(action).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
