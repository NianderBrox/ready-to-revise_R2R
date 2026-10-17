package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.AnalyticsHeatmapResponseDto
import com.r2r.readytorevise.data.remote.dto.AnalyticsOverviewResponseDto
import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApi {

    @GET("api/v1/analytics/overview")
    suspend fun getOverview(
        @Query("range") range: String
    ): BaseResponseDto<AnalyticsOverviewResponseDto>

    @GET("api/v1/analytics/heatmap")
    suspend fun getHeatmap(): BaseResponseDto<AnalyticsHeatmapResponseDto>
}
