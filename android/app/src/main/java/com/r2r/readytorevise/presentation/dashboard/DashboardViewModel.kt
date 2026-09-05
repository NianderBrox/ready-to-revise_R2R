package com.r2r.readytorevise.presentation.dashboard

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.data.remote.dueBeforeTodayISO
import com.r2r.readytorevise.domain.repository.AuthRepository
import com.r2r.readytorevise.domain.repository.DashboardRepository
import com.r2r.readytorevise.domain.repository.RecommendationsRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val recommendationsRepository: RecommendationsRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel<DashboardUiState, DashboardEvent, DashboardEffect>(DashboardUiState()) {

    init {
        load()
    }

    override fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.RetryLoad -> load()
            DashboardEvent.LogoutClicked -> logout()
        }
    }

    private fun load() {
        updateState { copy(loading = true, error = null) }

        viewModelScope.launch {
            coroutineScope {
                val dueBefore = dueBeforeTodayISO()

                val dashboardDeferred = async {
                    dashboardRepository.getDashboard(dueBefore)
                }
                val recommendationsDeferred = async {
                    recommendationsRepository.getRecommendations(limit = 5, dueBefore = dueBefore)
                }

                val dashboardResult = dashboardDeferred.await()
                val recommendationsResult = recommendationsDeferred.await()

                val dashboard = dashboardResult.getOrNull()

                if (dashboard == null) {
                    updateState {
                        copy(
                            loading = false,
                            error = dashboardResult.exceptionOrNull()?.message
                                ?: "Couldn't load your dashboard.",
                        )
                    }
                    return@coroutineScope
                }

                val recommendations = recommendationsResult.getOrNull()

                updateState {
                    copy(
                        loading = false,
                        error = null,
                        userName = dashboard.user.name,
                        readyToReviseCount = dashboard.reviews.slippingSoon,
                        dueToday = dashboard.reviews.dueToday,
                        upcoming = dashboard.reviews.upcoming,
                        completedToday = dashboard.reviews.completedToday,
                        streakDays = dashboard.progress.streakDays,
                        completionPercentage = dashboard.progress.completionPercentage,
                        studyItemsCount = dashboard.stats.studyItems,
                        aiSuggestion = dashboard.ai.suggestion,
                        recommendations = recommendations?.items.orEmpty(),
                        recommendationsSource = recommendations?.source,
                    )
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()

            sendEffect(DashboardEffect.LoggedOut)
        }
    }
}
