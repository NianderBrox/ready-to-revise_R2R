package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.AnalyticsApi
import com.r2r.readytorevise.data.remote.dto.AnalyticsHeatmapResponseDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsOverviewResponseDto
import com.r2r.readytorevise.domain.repository.AnalyticsRange
import com.r2r.readytorevise.domain.repository.AnalyticsRepository

class AnalyticsRepositoryImpl(
    private val analyticsApi: AnalyticsApi,
    private val tokenManager: TokenManager,
) : AnalyticsRepository {

    override suspend fun getOverview(range: AnalyticsRange): Result<AnalyticsOverviewResponseDto> {
        return safeCall(tokenManager) {
            analyticsApi.getOverview(range.raw).data
        }
    }

    override suspend fun getHeatmap(): Result<AnalyticsHeatmapResponseDto> {
        return safeCall(tokenManager) {
            analyticsApi.getHeatmap().data
        }
    }
}

