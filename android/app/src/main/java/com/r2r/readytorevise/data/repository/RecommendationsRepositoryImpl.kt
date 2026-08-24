package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.RecommendationsApi
import com.r2r.readytorevise.data.remote.dto.RecommendationsDto
import com.r2r.readytorevise.domain.repository.RecommendationsRepository

class RecommendationsRepositoryImpl(
    private val recommendationsApi: RecommendationsApi,
    private val tokenManager: TokenManager,
) : RecommendationsRepository {

    override suspend fun getRecommendations(limit: Int?): Result<RecommendationsDto> {
        return safeCall(tokenManager) { recommendationsApi.getRecommendations(limit).data }
    }
}
