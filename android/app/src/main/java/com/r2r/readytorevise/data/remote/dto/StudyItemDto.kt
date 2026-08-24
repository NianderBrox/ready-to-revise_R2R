package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StudyItemDto(
    val id: String,
    val title: String? = null,
    val content: String? = null,
    val type: String,
    val difficulty: String? = null,
    val topicId: String? = null,
    val options: List<String>? = null,
    val origin: String? = null,
    val nextReviewAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)
