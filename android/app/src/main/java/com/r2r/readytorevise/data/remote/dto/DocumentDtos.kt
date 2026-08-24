package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DocumentAnalysisDto(
    val extractedText: String,
    val title: String,
    val summary: String,
    val subject: String,
    val chapter: String,
    val topic: String,
    val difficulty: String,
    val keywords: List<String>
)

@Serializable
data class UploadDocumentDto(
    val documentId: String,
    val status: String,
    val analysis: DocumentAnalysisDto
)

@Serializable
data class DocumentDto(
    val id: String,
    val title: String? = null,
    val originalName: String,
    val mimeType: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String? = null
)
