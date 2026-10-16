package com.r2r.readytorevise.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.r2r.readytorevise.ui.components.common.R2RTopBar
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.SkyBlue

@Composable
fun SettingsRoute(
    preferences: SettingsPreferences,
    onBackClick: () -> Unit
) {
    val factory = viewModelFactory {
        initializer {
            SettingsViewModel(preferences = preferences)
        }
    }

    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            R2RTopBar(
                title = "Settings",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Auto Accept Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Skip the review step for questions you trust",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            AutoAcceptSegmentedSwitch(
                selected = state.autoAcceptMode,
                onSelect = { mode ->
                    onEvent(SettingsEvent.SetAutoAcceptMode(mode))
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.autoAcceptMode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun AutoAcceptSegmentedSwitch(
    selected: AutoAcceptMode,
    onSelect: (AutoAcceptMode) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val modes = AutoAcceptMode.entries

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, SkyBlue, shape)
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) SkyBlue else Color.White
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else SkyBlue
                )
            }
        }
    }
}
