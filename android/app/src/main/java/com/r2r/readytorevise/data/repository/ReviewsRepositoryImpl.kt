package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.ReviewsApi
import com.r2r.readytorevise.data.remote.dto.CreateReviewRequestDto
import com.r2r.readytorevise.data.remote.dto.ReviewDto
import com.r2r.readytorevise.data.remote.dto.SelfGradeReviewDto
import com.r2r.readytorevise.domain.repository.ReviewsRepository

class ReviewsRepositoryImpl(
    private val reviewsApi: ReviewsApi,
    private val tokenManager: TokenManager,
) : ReviewsRepository {

    override suspend fun submit(request: CreateReviewRequestDto): Result<ReviewDto> {
        return safeCall(tokenManager) { reviewsApi.create(request).data }
    }

    override suspend fun selfGrade(request: SelfGradeReviewDto): Result<ReviewDto> {
        return safeCall(tokenManager) { reviewsApi.selfGrade(request).data }
    }

    override suspend fun history(studyItemId: String): Result<List<ReviewDto>> {
        return safeCall(tokenManager) { reviewsApi.history(studyItemId).data }
    }
}
