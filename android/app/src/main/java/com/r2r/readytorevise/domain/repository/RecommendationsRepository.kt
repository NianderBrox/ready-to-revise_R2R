package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.RecommendationsDto

interface RecommendationsRepository {
    suspend fun getRecommendations(limit: Int? = null): Result<RecommendationsDto>
}
