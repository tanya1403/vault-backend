package com.vaultlink.app.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class LoginRequest(
    @field:NotBlank(message = "username is required")
    @field:Size(max = 150, message = "username must be at most 150 characters")
    val username: String,
    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: AuthenticatedUserResponse,
)

data class AuthenticatedUserResponse(
    val id: String,
    val username: String,
    val fullName: String,
    val roles: List<String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Instant = Instant.now(),
) {
    companion object {
        fun <T> success(message: String, data: T): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun failure(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)
    }
}

data class RegisterRequest(
    @field:NotBlank(message = "username is required")
    @field:Size(max = 150, message = "username must be at most 150 characters")
    val username: String,

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
    val password: String,

    @field:NotBlank(message = "fullName is required")
    @field:Size(max = 150, message = "fullName must be at most 150 characters")
    val fullName: String,

    @field:NotBlank(message = "role is required")
    val role: String,
)

