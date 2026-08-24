package com.r2r.readytorevise.presentation.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.OnSurface
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.SkyBlue

@Composable
fun AnalyticsRoute(
    appContainer: AppContainer,
    navController: NavController,
) {
    val factory = viewModelFactory {
        initializer {
            AnalyticsViewModel(appContainer.dashboardRepository)
        }
    }

    val viewModel: AnalyticsViewModel = viewModel(factory = factory)

    val state by viewModel.state.collectAsState()

    AnalyticsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        navController = navController,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onEvent: (AnalyticsEvent) -> Unit,
    navController: NavController,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyBlue)
            )
        }
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    state.error?.let { error ->
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { onEvent(AnalyticsEvent.RetryLoad) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Library",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatBlock("Items", state.studyItems)
                                    StatBlock("Subjects", state.subjects)
                                    StatBlock("Chapters", state.chapters)
                                    StatBlock("Topics", state.topics)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Reviews",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatBlock("Due today", state.dueToday)
                                    StatBlock("Upcoming", state.upcoming)
                                    StatBlock("Done today", state.completedToday)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Completion ${state.completionPercentage}% · " +
                                        "Streak ${state.streakDays}d",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }

                    if (state.recentActivity.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }

                        items(state.recentActivity) { activity ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activity.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )

                                    Text(
                                        text = formatDate(activity.reviewedAt),
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SkyBlue
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = OnSurfaceVariant
        )
    }
}

private fun formatDate(isoDate: String): String {
    return isoDate.take(10)
}
