package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateReviewRequestDto(
    val studyItemId: String,
    val selectedOptionIndex: Int,
    val responseTimeMs: Int? = null,
    val hesitationMs: Int? = null,
    val answerChanges: Int? = null,
    val sessionId: String? = null,
    val sessionDurationMinutes: Int? = null,
    val questionPositionInSession: Int? = null,
)
