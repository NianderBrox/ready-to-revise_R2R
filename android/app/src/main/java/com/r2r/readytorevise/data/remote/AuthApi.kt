package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.AuthResponseDto
import com.r2r.readytorevise.data.remote.dto.ChangePasswordRequestDto
import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.ForgotPasswordRequestDto
import com.r2r.readytorevise.data.remote.dto.LoginRequestDto
import com.r2r.readytorevise.data.remote.dto.ProfileDto
import com.r2r.readytorevise.data.remote.dto.MessageResponseDto
import com.r2r.readytorevise.data.remote.dto.RegisterRequestDto
import com.r2r.readytorevise.data.remote.dto.ResetPasswordRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): BaseResponseDto<AuthResponseDto>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): BaseResponseDto<AuthResponseDto>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto
    ): BaseResponseDto<MessageResponseDto>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto
    ): BaseResponseDto<MessageResponseDto>

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequestDto
    ): BaseResponseDto<MessageResponseDto>

    @GET("api/v1/auth/profile")
    suspend fun profile(): BaseResponseDto<ProfileDto>
}
