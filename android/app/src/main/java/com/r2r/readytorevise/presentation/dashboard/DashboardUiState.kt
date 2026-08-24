package com.r2r.readytorevise.presentation.dashboard

import com.r2r.readytorevise.data.remote.dto.RecommendationItemDto
import com.r2r.readytorevise.presentation.base.UiState

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val userName: String = "",
    val readyToReviseCount: Int = 0,
    val dueToday: Int = 0,
    val upcoming: Int = 0,
    val completedToday: Int = 0,
    val streakDays: Int = 0,
    val completionPercentage: Int = 0,
    val studyItemsCount: Int = 0,
    val aiSuggestion: String? = null,
    val recommendations: List<RecommendationItemDto> = emptyList(),
    val recommendationsSource: String? = null,
) : UiState {
    val topRecommendationTitle: String?
        get() = recommendations.firstOrNull()?.title
}
