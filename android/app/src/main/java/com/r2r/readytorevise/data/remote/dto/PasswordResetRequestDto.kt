package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)

@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val otp: String,
    val newPassword: String
)

@Serializable
data class MessageResponseDto(
    val message: String
)

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String
)
