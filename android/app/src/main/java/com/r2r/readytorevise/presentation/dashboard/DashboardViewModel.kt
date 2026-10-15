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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

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

                val loaded = try {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        dashboardDeferred.await() to recommendationsDeferred.await()
                    }
                } catch (e: TimeoutCancellationException) {
                    null
                }

                if (loaded == null) {
                    updateState {
                        copy(
                            loading = false,
                            error = "Taking too long. Check your connection and retry.",
                        )
                    }
                    return@coroutineScope
                }

                val (dashboardResult, recommendationsResult) = loaded

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

    private companion object {
        const val LOAD_TIMEOUT_MS = 90_000L
    }
}
