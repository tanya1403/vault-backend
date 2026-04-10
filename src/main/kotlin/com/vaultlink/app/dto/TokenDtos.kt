package com.vaultlink.app.dto

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "refreshToken is required")
    val refreshToken: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val tokenType: String
)

data class LogoutRequest(
    val refreshToken: String? = null
)

data class LogoutResponse(
    val message: String
)
