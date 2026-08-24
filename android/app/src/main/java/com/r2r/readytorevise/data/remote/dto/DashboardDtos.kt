package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardUserDto(
    val name: String
)

@Serializable
data class DashboardStatsDto(
    val studyItems: Int,
    val subjects: Int,
    val chapters: Int,
    val topics: Int,
    val inboxItems: Int
)

@Serializable
data class DashboardReviewsDto(
    val dueToday: Int,
    val upcoming: Int,
    val completedToday: Int,
    val slippingSoon: Int = 0
)

@Serializable
data class DashboardProgressDto(
    val completionPercentage: Int,
    val streakDays: Int
)

@Serializable
data class DashboardActivityDto(
    val title: String,
    val reviewedAt: String
)

@Serializable
data class DashboardAiDto(
    val suggestion: String? = null
)

@Serializable
data class DashboardResponseDto(
    val user: DashboardUserDto,
    val stats: DashboardStatsDto,
    val reviews: DashboardReviewsDto,
    val progress: DashboardProgressDto,
    val recentActivity: List<DashboardActivityDto> = emptyList(),
    val ai: DashboardAiDto
)
