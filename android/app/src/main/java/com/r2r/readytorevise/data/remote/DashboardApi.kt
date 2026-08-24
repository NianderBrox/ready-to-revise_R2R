package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.DashboardResponseDto
import retrofit2.http.GET

interface DashboardApi {

    @GET("api/v1/dashboard")
    suspend fun getDashboard(): BaseResponseDto<DashboardResponseDto>
}
