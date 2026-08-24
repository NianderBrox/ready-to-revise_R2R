package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.DocumentsApi
import com.r2r.readytorevise.data.remote.dto.DocumentDto
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.data.remote.dto.UploadDocumentDto
import com.r2r.readytorevise.domain.repository.DocumentsRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DocumentsRepositoryImpl(
    private val documentsApi: DocumentsApi,
    private val tokenManager: TokenManager,
) : DocumentsRepository {

    override suspend fun upload(file: File, mimeType: String): Result<UploadDocumentDto> {
        return safeCall(tokenManager) {
            val mediaType = mimeType.toMediaTypeOrNull()
                ?: throw IllegalArgumentException("Unsupported MIME type: $mimeType")
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mediaType),
            )
            documentsApi.upload(part).data
        }
    }

    override suspend fun getDocument(id: String): Result<DocumentDto> {
        return safeCall(tokenManager) { documentsApi.getDocument(id).data }
    }

    override suspend fun getDocuments(): Result<List<DocumentDto>> {
        return safeCall(tokenManager) { documentsApi.getDocuments().data }
    }

    override suspend fun generateQuestions(documentId: String): Result<List<StudyItemDto>> {
        return safeCall(tokenManager) { documentsApi.generateQuestions(documentId).data }
    }
}
