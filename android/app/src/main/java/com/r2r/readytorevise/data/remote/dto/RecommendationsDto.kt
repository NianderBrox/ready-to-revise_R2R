package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationItemDto(
    val studyItemId: String,
    val title: String? = null,
    val mediaDocumentId: String? = null,
    val options: List<String>? = null,
    val nextReviewAt: String? = null,
    val rank: Int
)

@Serializable
data class RecommendationsDto(
    val source: String,
    val items: List<RecommendationItemDto>
)