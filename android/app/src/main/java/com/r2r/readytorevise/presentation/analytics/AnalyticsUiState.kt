package com.r2r.readytorevise.presentation.analytics

import com.r2r.readytorevise.data.remote.dto.AnalyticsDailyPointDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsHeatmapDayDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsRecentRowDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsSubjectRowDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsSummaryRowDto
import com.r2r.readytorevise.domain.repository.AnalyticsRange
import com.r2r.readytorevise.presentation.base.UiState

data class AnalyticsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val range: AnalyticsRange = AnalyticsRange.SEVEN,
    val summary: AnalyticsSummaryRowDto = AnalyticsSummaryRowDto(
        totalReviews = 0,
        accuracyPct = 0,
        currentStreak = 0,
        longestStreak = 0,
        completionToday = 0,
        studyItems = 0,
        dueToday = 0,
    ),
    val daily: List<AnalyticsDailyPointDto> = emptyList(),
    val subjectRows: List<AnalyticsSubjectRowDto> = emptyList(),
    val recent: List<AnalyticsRecentRowDto> = emptyList(),
    val heatmapDays: List<AnalyticsHeatmapDayDto> = emptyList(),
    val heatmapStartDate: String = "",
    val heatmapDaysLoaded: Boolean = false,
    val heatmapError: Boolean = false,
) : UiState
