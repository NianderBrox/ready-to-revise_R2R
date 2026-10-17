package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsSummaryRowDto(
    val totalReviews: Int,
    val accuracyPct: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionToday: Int,
    val studyItems: Int,
    val dueToday: Int
)

@Serializable
data class AnalyticsDailyPointDto(
    val date: String,
    val completed: Int,
    val accuracyPct: Int,
    val minutes: Int
)

@Serializable
data class AnalyticsSubjectRowDto(
    val name: String,
    val items: Int,
    val due: Int,
    val reviews: Int,
    val accuracyPct: Int
)

@Serializable
data class AnalyticsRecentRowDto(
    val title: String,
    val reviewedAt: String,
    val isCorrect: Boolean? = null
)

@Serializable
data class AnalyticsOverviewResponseDto(
    val range: String,
    val summary: AnalyticsSummaryRowDto,
    val daily: List<AnalyticsDailyPointDto>,
    val subjects: List<AnalyticsSubjectRowDto>,
    val recent: List<AnalyticsRecentRowDto>
)

@Serializable
data class AnalyticsHeatmapDayDto(
    val date: String,
    val completed: Int,
    val level: Int
)

@Serializable
data class AnalyticsHeatmapResponseDto(
    val startDate: String,
    val days: List<AnalyticsHeatmapDayDto>
)
