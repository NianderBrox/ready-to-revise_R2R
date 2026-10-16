package com.r2r.readytorevise.presentation.processing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.navigation.Screen
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.theme.Success

@Composable
fun UploadSuccessRoute(
    uploadViewModel: UploadViewModel,
    navController: NavController,
) {
    val state by uploadViewModel.state.collectAsState()

    val goToDashboard: () -> Unit = {
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.UploadSuccess.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    BackHandler(onBack = goToDashboard)

    UploadSuccessScreen(
        keptCount = state.lastSavedCount,
        onBackToDashboard = goToDashboard,
    )
}

@Composable
fun UploadSuccessScreen(
    keptCount: Int,
    onBackToDashboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(80.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Questions saved",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$keptCount question${if (keptCount == 1) "" else "s"} accepted into your deck",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        R2RButton(
            text = "Back to Dashboard",
            onClick = onBackToDashboard,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
