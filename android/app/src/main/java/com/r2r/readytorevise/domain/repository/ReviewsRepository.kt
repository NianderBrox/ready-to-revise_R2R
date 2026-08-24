package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.CreateReviewRequestDto
import com.r2r.readytorevise.data.remote.dto.ReviewDto
import com.r2r.readytorevise.data.remote.dto.SelfGradeReviewDto

interface ReviewsRepository {
    suspend fun submit(request: CreateReviewRequestDto): Result<ReviewDto>

    suspend fun selfGrade(request: SelfGradeReviewDto): Result<ReviewDto>

    suspend fun history(studyItemId: String): Result<List<ReviewDto>>
}
