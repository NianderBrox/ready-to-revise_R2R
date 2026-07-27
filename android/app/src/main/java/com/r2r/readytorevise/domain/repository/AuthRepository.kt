package com.r2r.readytorevise.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
}
