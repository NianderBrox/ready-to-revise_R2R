package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.DashboardResponseDto

interface DashboardRepository {
    suspend fun getDashboard(): Result<DashboardResponseDto>
}
