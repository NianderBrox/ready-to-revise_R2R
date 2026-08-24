package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationItemDto(
    val studyItemId: String,
    val title: String? = null,
    val mediaDocumentId: String? = null,
    val options: List<String>? = null,
    val expectedForgetDate: String? = null,
    val recallProbability: Double? = null,
    val priority: String? = null,
    val rank: Int
)

@Serializable
data class RecommendationMetaDto(
    val restingNow: Int = 0,
    val upcomingLater: Int = 0
)

@Serializable
data class RecommendationsDto(
    val source: String,
    val items: List<RecommendationItemDto>,
    val meta: RecommendationMetaDto? = null
)
