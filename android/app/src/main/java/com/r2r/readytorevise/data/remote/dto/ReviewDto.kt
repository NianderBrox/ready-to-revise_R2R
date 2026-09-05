package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String,
    val studyItemId: String,
    val result: String,
    val intervalDays: Double,
    val reviewedAt: String,
    val nextReviewAt: String,
    val isCorrect: Boolean? = null,
    val confidenceScore: Double? = null,
    val createdAt: String,
    val updatedAt: String
)
