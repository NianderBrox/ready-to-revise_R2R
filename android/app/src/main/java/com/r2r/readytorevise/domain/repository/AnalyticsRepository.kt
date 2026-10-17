package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.AnalyticsHeatmapResponseDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsOverviewResponseDto

enum class AnalyticsRange(val raw: String) {
    SEVEN("7d"),
    THIRTY("30d"),
}

interface AnalyticsRepository {
    suspend fun getOverview(range: AnalyticsRange): Result<AnalyticsOverviewResponseDto>
    suspend fun getHeatmap(): Result<AnalyticsHeatmapResponseDto>
}
