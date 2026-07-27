package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.AuthApi
import com.r2r.readytorevise.data.remote.dto.LoginRequestDto
import com.r2r.readytorevise.data.remote.dto.RegisterRequestDto
import com.r2r.readytorevise.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenManager.token.map { !it.isNullOrBlank() }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        return try {
            val response = authApi.register(RegisterRequestDto(name, email, password))
            tokenManager.saveToken(response.data.accessToken)
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception("Registration failed: ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApi.login(LoginRequestDto(email, password))
            tokenManager.saveToken(response.data.accessToken)
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception("Login failed. Please check your credentials."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }
}
