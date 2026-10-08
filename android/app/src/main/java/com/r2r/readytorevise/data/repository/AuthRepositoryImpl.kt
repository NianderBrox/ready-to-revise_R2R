package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.AuthApi
import com.r2r.readytorevise.data.remote.dto.LoginRequestDto
import com.r2r.readytorevise.data.remote.dto.ProfileDto
import com.r2r.readytorevise.data.remote.dto.ChangePasswordRequestDto
import com.r2r.readytorevise.data.remote.dto.ForgotPasswordRequestDto
import com.r2r.readytorevise.data.remote.dto.RegisterRequestDto
import com.r2r.readytorevise.data.remote.dto.ResetPasswordRequestDto
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
            authApi.register(RegisterRequestDto(name, email, password))
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
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }

    override suspend fun profile(): Result<ProfileDto> {
        return try {
            Result.success(authApi.profile().data)
        } catch (e: HttpException) {
            if (e.code() == 401) tokenManager.clearToken()
            Result.failure(Exception("Couldn't load profile."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }

    override suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = authApi.forgotPassword(ForgotPasswordRequestDto(email))
            Result.success(response.data.message)
        } catch (e: HttpException) {
            Result.failure(Exception(serverMessage(e) ?: "Couldn't send reset code."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }

    override suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<String> {
        return try {
            val response = authApi.resetPassword(ResetPasswordRequestDto(email, otp, newPassword))
            Result.success(response.data.message)
        } catch (e: HttpException) {
            Result.failure(Exception(serverMessage(e) ?: "Couldn't reset password."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> {
        return try {
            val response = authApi.changePassword(ChangePasswordRequestDto(currentPassword, newPassword))
            Result.success(response.data.message)
        } catch (e: HttpException) {
            Result.failure(Exception(serverMessage(e) ?: "Couldn't change password."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred."))
        }
    }

    private fun serverMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string()
            body?.let {
                val match = Regex(""""message"\s*:\s*"([^"]+)"""").find(it)
                match?.groupValues?.get(1)
            }
        } catch (t: Throwable) {
            null
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            tokenManager.clearToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Logout failed."))
        }
    }
}
