package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T
)
