package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.StudyItemsApi
import com.r2r.readytorevise.data.remote.dto.StudyItemDto
import com.r2r.readytorevise.domain.repository.StudyItemsRepository

class StudyItemsRepositoryImpl(
    private val studyItemsApi: StudyItemsApi,
    private val tokenManager: TokenManager,
) : StudyItemsRepository {

    override suspend fun getStudyItems(
        type: String?,
        due: Boolean?,
    ): Result<List<StudyItemDto>> {
        return safeCall(tokenManager) { studyItemsApi.getStudyItems(type, due).data }
    }

    override suspend fun getStudyItem(id: String): Result<StudyItemDto> {
        return safeCall(tokenManager) { studyItemsApi.getStudyItem(id).data }
    }

    override suspend fun deleteStudyItem(id: String): Result<Unit> {
        return safeCall(tokenManager) {
            studyItemsApi.deleteStudyItem(id).data
        }
    }
}
