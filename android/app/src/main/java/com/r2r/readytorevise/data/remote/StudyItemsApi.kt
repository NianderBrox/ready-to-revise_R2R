package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StudyItemsApi {

    @GET("api/v1/study-items")
    suspend fun getStudyItems(
        @Query("type") type: String? = null,
        @Query("due") due: Boolean? = null
    ): BaseResponseDto<List<StudyItemDto>>

    @GET("api/v1/study-items/{id}")
    suspend fun getStudyItem(
        @Path("id") id: String
    ): BaseResponseDto<StudyItemDto>

    @DELETE("api/v1/study-items/{id}")
    suspend fun deleteStudyItem(
        @Path("id") id: String
    ): BaseResponseDto<Unit>
}
