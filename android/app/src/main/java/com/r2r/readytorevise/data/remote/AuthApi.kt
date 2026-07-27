package com.r2r.readytorevise.data.remote

import com.r2r.readytorevise.data.remote.dto.AuthResponseDto
import com.r2r.readytorevise.data.remote.dto.BaseResponseDto
import com.r2r.readytorevise.data.remote.dto.LoginRequestDto
import com.r2r.readytorevise.data.remote.dto.RegisterRequestDto
import retrofit2.http.Body
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
}
