package com.r2r.readytorevise.presentation.analytics

import com.r2r.readytorevise.data.remote.dto.DashboardActivityDto
import com.r2r.readytorevise.presentation.base.UiState

data class AnalyticsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val studyItems: Int = 0,
    val subjects: Int = 0,
    val chapters: Int = 0,
    val topics: Int = 0,
    val dueToday: Int = 0,
    val upcoming: Int = 0,
    val completedToday: Int = 0,
    val completionPercentage: Int = 0,
    val streakDays: Int = 0,
    val recentActivity: List<DashboardActivityDto> = emptyList(),
) : UiState
