package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.DocumentDto
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.data.remote.dto.UploadDocumentDto
import java.io.File

interface DocumentsRepository {
    suspend fun upload(file: File, mimeType: String): Result<UploadDocumentDto>

    suspend fun getDocument(id: String): Result<DocumentDto>

    suspend fun getDocuments(): Result<List<DocumentDto>>

    suspend fun generateQuestions(documentId: String): Result<List<StudyItemDto>>
}
