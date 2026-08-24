package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SelfGradeReviewDto(
    val studyItemId: String,
    val responseTimeMs: Int? = null,
    val sessionId: String? = null
)
