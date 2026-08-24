package com.r2r.readytorevise.data.repository

import com.r2r.readytorevise.data.local.TokenManager
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeCall(
    tokenManager: TokenManager? = null,
    block: suspend () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: HttpException) {
        if (e.code() == 401) {
            tokenManager?.clearToken()
            Result.failure(SessionExpiredException())
        } else {
            Result.failure(Exception(apiErrorMessage(e)))
        }
    } catch (e: IOException) {
        Result.failure(Exception("Network error. Please check your connection."))
    } catch (e: Exception) {
        Result.failure(Exception("An unexpected error occurred."))
    }
}

class SessionExpiredException : Exception("Session expired. Please log in again.")

private fun apiErrorMessage(e: HttpException): String {
    return when (e.code()) {
        400 -> "Invalid request."
        403 -> "Not allowed."
        404 -> "Not found."
        413 -> "File too large (max 20 MB)."
        503 -> "Service temporarily unavailable."
        in 500..599 -> "Server error. Please try again later."
        else -> "Request failed (${e.code()})."
    }
}
