package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.CreateReviewRequestDto
import com.r2r.readytorevise.data.remote.dto.ReviewDto
import com.r2r.readytorevise.data.remote.dto.SelfGradeReviewDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReviewsApi {

    @POST("api/v1/reviews")
    suspend fun create(
        @Body request: CreateReviewRequestDto
    ): BaseResponseDto<ReviewDto>

    @POST("api/v1/reviews/self-grade")
    suspend fun selfGrade(
        @Body request: SelfGradeReviewDto
    ): BaseResponseDto<ReviewDto>

    @GET("api/v1/reviews/study-items/{studyItemId}")
    suspend fun history(
        @Path("studyItemId") studyItemId: String
    ): BaseResponseDto<List<ReviewDto>>
}
