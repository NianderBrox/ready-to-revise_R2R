package com.r2r.readytorevise.presentation.processing

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.presentation.upload.UploadEffect
import com.r2r.readytorevise.presentation.upload.UploadEvent
import com.r2r.readytorevise.presentation.upload.UploadStage
import com.r2r.readytorevise.presentation.upload.UploadUiState
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import com.r2r.readytorevise.ui.theme.OnSurface
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.SkyBlue
import com.r2r.readytorevise.ui.theme.Success
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.navigation.Screen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.alpha
import com.r2r.readytorevise.data.remote.dto.StudyItemDto

private val AccentGreen = androidx.compose.ui.graphics.Color(0xFF22C55E)

@Composable
fun ProcessingRoute(
    uploadViewModel: UploadViewModel,
    navController: NavController,
) {
    val state by uploadViewModel.state.collectAsState()

    LaunchedEffect(uploadViewModel) {
        uploadViewModel.effect.collect { effect ->
            when (effect) {
                is UploadEffect.ShowToast -> {
                    Toast.makeText(
                        navController.context,
                        effect.message,
                        Toast.LENGTH_LONG,
                    ).show()
                }

                UploadEffect.NavigateToDashboard -> {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Processing.route) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }

    ProcessingScreen(
        state = state,
        onRetry = { uploadViewModel.onEvent(UploadEvent.Submit) },
        onBack = { navController.popBackStack() },
        onEvent = uploadViewModel::onEvent,
    )
}

@Composable
fun ProcessingScreen(
    state: UploadUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onEvent: (UploadEvent) -> Unit = {},
) {
    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(state.stage) {
        if (state.stage == UploadStage.DONE) {
            checkScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.stage) {
            UploadStage.IDLE, UploadStage.WORKING -> {
                CircularProgressIndicator(color = SkyBlue)

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Processing your notes...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.totalItems > 0) {
                    LinearProgressIndicator(
                        progress = {
                            state.completedItems.toFloat() / state.totalItems.toFloat()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = SkyBlue,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "File ${state.completedItems + 1} of ${state.totalItems}",
                        color = OnSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.statusLine.isNotEmpty()) {
                    Text(state.statusLine, color = OnSurface)
                }
            }

            UploadStage.DONE -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = checkScale.value
                            scaleY = checkScale.value
                        },
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Review your questions",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.statusLine.ifEmpty {
                        "${state.generatedCount} generated — discard anything off-topic"
                    },
                    color = OnSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    state.generatedItems
                        .sortedByDescending { it.origin == "EXTRACTED" }
                        .forEach { item ->
                            CurationCard(
                                item = item,
                                discarded = state.discardedIds.contains(item.id),
                                onToggle = {
                                    onEvent(
                                        UploadEvent.ToggleDiscard(item.id),
                                    )
                                },
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                    if (state.failureLines.isNotEmpty()) {
                        Text(state.failureLines.joinToString("\n"), color = OnSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onEvent(UploadEvent.ConfirmCuration) },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                    enabled = !state.curationBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.curationBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = OnSurface,
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Removing discarded…", color = OnSurface)
                    } else {
                        Text("Keep ${state.keptCount} · Confirm")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onEvent(UploadEvent.SkipCuration) },
                    enabled = !state.curationBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Keep all", color = OnSurface)
                }
            }

            UploadStage.ERROR -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(80.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Upload failed",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.failureLines.isNotEmpty()) {
                    Text(
                        text = state.failureLines.joinToString("\n"),
                        color = OnSurfaceVariant,
                    )
                } else if (state.statusLine.isNotEmpty()) {
                    Text(state.statusLine, color = OnSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Try again")
                }

                state.errorCountdownSeconds?.let { seconds ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Returning to dashboard in $seconds…",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Background),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Back")
                }
            }
        }
    }
}


@Composable
private fun CurationCard(
    item: StudyItemDto,
    discarded: Boolean,
    onToggle: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Background,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (discarded) 0.5f else 1f)
            .border(
                width = if (discarded) 1.dp else 0.dp,
                color = AccentGreen,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (item.origin) {
                        "EXTRACTED" -> "EXTRACTED"
                        else -> "AI-GENERATED"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.origin == "EXTRACTED") Success else SkyBlue,
                    modifier = Modifier
                        .alpha(1f),
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (discarded) "Discard ✓" else "Tap to discard",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(item.title ?: "Untitled question", color = OnSurface)

            item.options?.takeIf { it.isNotEmpty() }?.let { options ->
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = options.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
