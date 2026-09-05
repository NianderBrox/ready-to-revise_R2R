package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.RecommendationsDto
import retrofit2.http.GET
import retrofit2.http.Query

interface RecommendationsApi {

    @GET("api/v1/recommendations")
    suspend fun getRecommendations(
        @Query("limit") limit: Int? = null,
        @Query("subjectId") subjectId: String? = null,
        @Query("dueBefore") dueBefore: String? = null
    ): BaseResponseDto<RecommendationsDto>
}
