package com.r2r.readytorevise.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.data.remote.dto.AnalyticsDailyPointDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsHeatmapDayDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsSubjectRowDto
import com.r2r.readytorevise.domain.repository.AnalyticsRange
import com.r2r.readytorevise.ui.components.common.AnalyticsSkeleton
import com.r2r.readytorevise.ui.components.common.EmptyState
import com.r2r.readytorevise.ui.components.common.R2RSegmentedSwitch
import com.r2r.readytorevise.ui.components.common.R2RTopBar

@Composable
fun AnalyticsRoute(
    appContainer: AppContainer,
    navController: NavController,
) {
    val factory = viewModelFactory {
        initializer {
            AnalyticsViewModel(appContainer.analyticsRepository)
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

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onEvent: (AnalyticsEvent) -> Unit,
    navController: NavController,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            R2RTopBar(
                title = "Analytics",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        when {
            state.loading -> {
                AnalyticsSkeleton(
                    modifier = Modifier.padding(padding)
                )
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
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { onEvent(AnalyticsEvent.RetryLoad) }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        R2RSegmentedSwitch(
                            options = listOf(AnalyticsRange.SEVEN, AnalyticsRange.THIRTY),
                            label = { if (it == AnalyticsRange.SEVEN) "7 days" else "30 days" },
                            selected = state.range,
                            onSelect = { onEvent(AnalyticsEvent.SetRange(it)) }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsStatTile(
                                value = state.summary.totalReviews.toString(),
                                label = "Reviews",
                                icon = Icons.Default.MenuBook,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatTile(
                                value = "${state.summary.accuracyPct}%",
                                label = "Accuracy",
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsStatTile(
                                value = "${state.summary.currentStreak}d",
                                label = "Streak",
                                icon = Icons.Default.LocalFireDepartment,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatTile(
                                value = "${state.summary.completionToday}%",
                                label = "Today's goal",
                                icon = Icons.Default.CalendarToday,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        ActivityChartCard(
                            daily = state.daily
                        )
                    }

                    if (state.heatmapDays.isNotEmpty()) {
                        item {
                            HeatmapCard(
                                days = state.heatmapDays,
                                startDate = state.heatmapStartDate
                            )
                        }
                    }

                    item {
                        StudyTimeCard(daily = state.daily)
                    }

                    if (state.subjectRows.isNotEmpty()) {
                        item { SectionTitle("Subjects") }
                        items(state.subjectRows) { row ->
                            SubjectRow(row = row)
                        }
                    }

                    if (state.recent.isNotEmpty()) {
                        item { SectionTitle("Recent activity") }
                        items(state.recent) { row ->
                            RecentRow(
                                title = row.title,
                                reviewedAt = row.reviewedAt,
                                isCorrect = row.isCorrect,
                            )
                        }
                    } else {
                        item {
                            EmptyState(
                                icon = Icons.Default.MenuBook,
                                title = "No activity yet",
                                message = "Complete a revision to see it here",
                                actionLabel = "Back to dashboard",
                                onAction = { navController.popBackStack() }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatTile(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityChartCard(
    daily: List<AnalyticsDailyPointDto>,
) {
    val maxCompleted = remember(daily) {
        daily.maxOfOrNull { it.completed } ?: 0
    }
    val rangeLabel = remember(daily) {
        val first = daily.firstOrNull()?.date ?: ""
        val last = daily.lastOrNull()?.date ?: ""
        if (first.isEmpty()) "" else "$first → $last"
    }
    val totalReviews = remember(daily) { daily.sumOf { it.completed } }
    val chartDescription = "Daily reviews bar chart, $rangeLabel, " +
        "$totalReviews reviews in this range"

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (totalReviews == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No reviews in this range yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val barColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .semantics { contentDescription = chartDescription }
                ) {
                    val slot = size.width / daily.size
                    val barWidth = slot * 0.55f
                    val cornerRadius = CornerRadius(6f, 6f)
                    daily.forEachIndexed { index, point ->
                        if (point.completed <= 0) {
                            return@forEachIndexed
                        }
                        val barHeight = if (maxCompleted > 0) {
                            (point.completed / maxCompleted.toFloat()) *
                                (size.height - 10f)
                        } else {
                            0f
                        }
                        val height = barHeight.coerceAtLeast(8f)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(
                                x = index * slot + (slot - barWidth) / 2f,
                                y = size.height - height
                            ),
                            size = Size(width = barWidth, height = height),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyTimeCard(
    daily: List<AnalyticsDailyPointDto>,
) {
    val totalMinutes = remember(daily) { daily.sumOf { it.minutes } }
    val activeDays = remember(daily) { daily.count { it.completed > 0 } }
    val averageMinutes = if (activeDays > 0) totalMinutes / activeDays else 0

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Study time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${totalMinutes}m in this range",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "avg ${averageMinutes}m on active days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeatmapCard(
    days: List<AnalyticsHeatmapDayDto>,
    startDate: String,
) {
    val pitchDp = 15
    val cellDp = 12
    val padCells = remember(startDate) {
        try {
            val date = java.time.LocalDate.parse(startDate)
            date.dayOfWeek.value - 1
        } catch (e: IllegalArgumentException) {
            0
        }
    }
    val columns = remember(days, padCells) {
        (padCells + days.size + 6) / 7
    }
    val ladder = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
        MaterialTheme.colorScheme.primary,
    )
    var selectedCaption by remember { mutableStateOf<String?>(null) }
    val gridDescription = "Last 365 days review heatmap, " +
        days.sumOf { it.completed } + " reviews total"

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Last 365 days",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedCaption
                    ?: "Show-up over the year",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Canvas(
                    modifier = Modifier
                        .width((columns * pitchDp - (pitchDp - cellDp)).dp)
                        .height((7 * pitchDp - (pitchDp - cellDp)).dp)
                        .pointerInput(days) {
                            val pitchPixels = pitchDp.dp.toPx()
                            detectTapGestures { offset ->
                                val col = (offset.x / pitchPixels).toInt()
                                val rowIdx = (offset.y / pitchPixels).toInt()
                                val index = col * 7 + rowIdx - padCells
                                val localDay = days.getOrNull(index)
                                selectedCaption = localDay?.let { day ->
                                    "${day.date}: ${day.completed} review" +
                                        (if (day.completed == 1) "" else "s")
                                }
                            }
                        }
                        .semantics { contentDescription = gridDescription }
                ) {
                    val pitchPixels = pitchDp.dp.toPx()
                    val cellPixels = cellDp.dp.toPx()
                    days.forEachIndexed { index, day ->
                        val cellIndex = padCells + index
                        val columnIdx = cellIndex / 7
                        val rowIdx = cellIndex % 7
                        drawRoundRect(
                            color = ladder[day.level.coerceIn(0, 4)],
                            topLeft = Offset(
                                x = columnIdx * pitchPixels,
                                y = rowIdx * pitchPixels
                            ),
                            size = Size(cellPixels, cellPixels),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SubjectRow(
    row: AnalyticsSubjectRowDto,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = if (row.due > 0) "${row.due} due" else "all caught up",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { row.accuracyPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics {
                        contentDescription =
                            "${row.name} accuracy ${row.accuracyPct} percent"
                    },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${row.reviews} reviews · ${row.accuracyPct}% accuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentRow(
    title: String,
    reviewedAt: String,
    isCorrect: Boolean?,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (isCorrect) {
                    true -> "Correct"
                    false -> "Wrong"
                    null -> "Read"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (isCorrect) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = formatDate(reviewedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(isoDate: String): String {
    return isoDate.take(10)
}
