package com.r2r.readytorevise.domain.repository

import com.r2r.readytorevise.data.remote.dto.ProfileDto
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun profile(): Result<ProfileDto>
    suspend fun logout(): Result<Unit>
}
