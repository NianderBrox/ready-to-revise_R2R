package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.DashboardApi
import com.r2r.readytorevise.data.remote.dto.DashboardResponseDto
import com.r2r.readytorevise.domain.repository.DashboardRepository

class DashboardRepositoryImpl(
    private val dashboardApi: DashboardApi,
    private val tokenManager: TokenManager,
) : DashboardRepository {

    override suspend fun getDashboard(dueBefore: String?): Result<DashboardResponseDto> {
        return safeCall(tokenManager) {
            dashboardApi.getDashboard(dueBefore).data
        }
    }
}
