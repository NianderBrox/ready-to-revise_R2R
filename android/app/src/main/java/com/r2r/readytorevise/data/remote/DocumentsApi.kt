package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.DocumentDto
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.data.remote.dto.UploadDocumentDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface DocumentsApi {

    @Multipart
    @POST("api/v1/documents")
    suspend fun upload(
        @Part file: MultipartBody.Part
    ): BaseResponseDto<UploadDocumentDto>

    @GET("api/v1/documents")
    suspend fun getDocuments(): BaseResponseDto<List<DocumentDto>>

    @GET("api/v1/documents/{id}")
    suspend fun getDocument(
        @Path("id") id: String
    ): BaseResponseDto<DocumentDto>

    @POST("api/v1/documents/{documentId}/questions")
    suspend fun generateQuestions(
        @Path("documentId") documentId: String
    ): BaseResponseDto<List<StudyItemDto>>

    @GET("api/v1/documents/{id}/file")
    suspend fun downloadFile(
        @Path("id") id: String
    ): ResponseBody
}
