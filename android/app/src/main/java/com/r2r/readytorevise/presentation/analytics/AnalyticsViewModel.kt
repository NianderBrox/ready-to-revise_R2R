package com.r2r.readytorevise.presentation.analytics

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.repository.AnalyticsRange
import com.r2r.readytorevise.domain.repository.AnalyticsRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import com.r2r.readytorevise.presentation.base.UiEvent
import kotlinx.coroutines.launch

sealed interface AnalyticsEvent : UiEvent {
    data object RetryLoad : AnalyticsEvent
    data class SetRange(val range: AnalyticsRange) : AnalyticsEvent
}

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
) : BaseViewModel<AnalyticsUiState, AnalyticsEvent, Nothing>(AnalyticsUiState()) {

    init {
        load()
        loadHeatmap()
    }

    override fun onEvent(event: AnalyticsEvent) {
        when (event) {
            AnalyticsEvent.RetryLoad -> load()
            is AnalyticsEvent.SetRange -> setRange(event.range)
        }
    }

    private fun setRange(range: AnalyticsRange) {
        if (range == state.value.range) {
            return
        }
        updateState { copy(range = range) }
        load()
    }

    private fun load() {
        updateState { copy(loading = true, error = null) }

        viewModelScope.launch {
            analyticsRepository.getOverview(state.value.range)
                .onSuccess { overview ->
                    updateState {
                        copy(
                            loading = false,
                            error = null,
                            summary = overview.summary,
                            daily = overview.daily,
                            subjectRows = overview.subjects,
                            recent = overview.recent,
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

    private fun loadHeatmap() {
        updateState { copy(heatmapError = false) }

        viewModelScope.launch {
            analyticsRepository.getHeatmap()
                .onSuccess { heatmap ->
                    updateState {
                        copy(
                            heatmapDays = heatmap.days,
                            heatmapStartDate = heatmap.startDate,
                            heatmapDaysLoaded = true,
                            heatmapError = false,
                        )
                    }
                }
                .onFailure {
                    updateState {
                        copy(heatmapDaysLoaded = true, heatmapError = true)
                    }
                }
        }
    }
}
