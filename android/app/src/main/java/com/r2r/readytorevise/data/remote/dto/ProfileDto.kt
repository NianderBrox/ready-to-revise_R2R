package com.r2r.readytorevise.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val userId: String,
    val email: String
)
