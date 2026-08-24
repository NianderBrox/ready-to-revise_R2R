package com.r2r.readytorevise.presentation.analytics

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.repository.DashboardRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import com.r2r.readytorevise.presentation.base.UiEvent
import kotlinx.coroutines.launch

sealed interface AnalyticsEvent : UiEvent {
    data object RetryLoad : AnalyticsEvent
}

class AnalyticsViewModel(
    private val dashboardRepository: DashboardRepository,
) : BaseViewModel<AnalyticsUiState, AnalyticsEvent, Nothing>(AnalyticsUiState()) {

    init {
        load()
    }

    override fun onEvent(event: AnalyticsEvent) {
        when (event) {
            AnalyticsEvent.RetryLoad -> load()
        }
    }

    private fun load() {
        updateState { copy(loading = true, error = null) }

        viewModelScope.launch {
            dashboardRepository.getDashboard()
                .onSuccess { dashboard ->
                    updateState {
                        copy(
                            loading = false,
                            error = null,
                            studyItems = dashboard.stats.studyItems,
                            subjects = dashboard.stats.subjects,
                            chapters = dashboard.stats.chapters,
                            topics = dashboard.stats.topics,
                            dueToday = dashboard.reviews.dueToday,
                            upcoming = dashboard.reviews.upcoming,
                            completedToday = dashboard.reviews.completedToday,
                            completionPercentage = dashboard.progress.completionPercentage,
                            streakDays = dashboard.progress.streakDays,
                            recentActivity = dashboard.recentActivity,
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            loading = false,
                            error = throwable.message ?: "Couldn't load analytics.",
                        )
                    }
                }
        }
    }
}
