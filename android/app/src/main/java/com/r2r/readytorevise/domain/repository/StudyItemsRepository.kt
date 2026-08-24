package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.StudyItemDto

interface StudyItemsRepository {
    suspend fun getStudyItems(
        type: String? = null,
        due: Boolean? = null,
    ): Result<List<StudyItemDto>>

    suspend fun getStudyItem(id: String): Result<StudyItemDto>

    suspend fun deleteStudyItem(id: String): Result<Unit>
}
