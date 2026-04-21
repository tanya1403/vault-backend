package com.vaultlink.app.service

import com.vaultlink.app.dto.LoginRequest
import com.vaultlink.app.dto.LoginResponse
import com.vaultlink.app.dto.RefreshTokenRequest
import com.vaultlink.app.dto.RefreshTokenResponse
import com.vaultlink.app.dto.UpdatePickupRequest
import com.vaultlink.app.dto.ApiResponse

import com.vaultlink.app.manager.SalesforceManager
import com.vaultlink.app.model.LoginAudit
import com.vaultlink.app.model.RefreshToken
import com.vaultlink.app.model.BlacklistedToken
import com.vaultlink.app.repository.LoginAuditRepository
import com.vaultlink.app.repository.UserRepository
import com.vaultlink.app.repository.RefreshTokenRepository
import com.vaultlink.app.repository.BlacklistedTokenRepository
import com.vaultlink.app.security.JwtService
import com.vaultlink.app.utills.LoggerUtils.log
import jakarta.servlet.http.HttpServletRequest
import org.json.JSONObject
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@Service
class VaultService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val loginAuditRepository: LoginAuditRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val blacklistedTokenRepository: BlacklistedTokenRepository,
    private val jwtService: JwtService,
    @Autowired val sfManager: SalesforceManager
) {

    private val logger = LoggerFactory.getLogger(VaultService::class.java)

    @Transactional
    fun login(
        request: LoginRequest,
//        httpRequest: HttpServletRequest,
    ): LoginResponse {
        val normalizedUsername = request.username.trim()

//        val authentication = try {
//            authenticationManager.authenticate(
//                UsernamePasswordAuthenticationToken(normalizedUsername, request.password)
//            )
//        } catch (_: DisabledException) {
//            throw BadCredentialsException("Invalid username or password")
//        } catch (_: BadCredentialsException) {
//            throw BadCredentialsException("Invalid username or password")
//        }

//        if (!authentication.isAuthenticated) {
//            throw BadCredentialsException("Invalid username or password")
//        }

//        val user = userRepository.findByUsernameIgnoreCase(normalizedUsername)
//            ?: throw BadCredentialsException("Invalid username or password")

//        if (!user.enabled) {
//            throw BadCredentialsException("Invalid username or password")
//        }
//
//        user.lastLoginAt = Instant.now()
//        userRepository.save(user)

//        loginAuditRepository.save(
//            LoginAudit(
//                user = user,
//                ipAddress = extractClientIp(httpRequest),
//                userAgent = httpRequest.getHeader("User-Agent"),
//            )
//        )

        // Revoke all existing refresh tokens for this user
//        refreshTokenRepository.revokeAllUserTokens(user.id!!, LocalDateTime.now())

        // Generate new tokens
//        val accessToken = jwtService.generateToken(user)
//        val refreshToken = jwtService.generateRefreshToken()
        // Store refresh token in database
//        val refreshTokenEntity = RefreshToken(
//            token = refreshToken,
//            user = user,
//            expiresAt = LocalDateTime.now().plusSeconds(jwtService.refreshExpirationSeconds),
//            createdAt = LocalDateTime.now()
//        )
//        refreshTokenRepository.save(refreshTokenEntity)

        return LoginResponse(
            accessToken = "accessToken",
            refreshToken = "refreshToken",
            expiresIn = jwtService.expirationSeconds,
            user = com.vaultlink.app.dto.AuthenticatedUserResponse(
                id = "user.id!!",
                username = "user.username",
                fullName = "user.fullName",
                roles = listOf(),
            ),
        )
    }

    @Transactional
    fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        val storedRefreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            .orElseThrow { BadCredentialsException("Invalid refresh token") }

        if (storedRefreshToken.isRevoked || storedRefreshToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw BadCredentialsException("Refresh token expired or revoked")
        }

        val user = storedRefreshToken.user
        if (!user.enabled) {
            throw BadCredentialsException("User account is disabled")
        }

        // Generate new access token
        val newAccessToken = jwtService.generateToken(user)

        // Update last used timestamp
        val updatedRefreshToken = storedRefreshToken.copy(
            lastUsedAt = LocalDateTime.now()
        )
        refreshTokenRepository.save(updatedRefreshToken)

        return RefreshTokenResponse(
            accessToken = newAccessToken,
            expiresIn = jwtService.expirationSeconds,
            tokenType = "Bearer"
        )
    }

    @Transactional
    fun logout(accessToken: String, refreshToken: String?) {
        try {
            val jti = jwtService.extractJti(accessToken)
            val userId = jwtService.extractUserId(accessToken)
            val expiresAt = LocalDateTime.now().plusSeconds(jwtService.expirationSeconds)

            // Blacklist access token
            if (jti != null && userId != null) {
                blacklistedTokenRepository.save(
                    BlacklistedToken(
                        jti = jti,
                        userId = userId,
                        tokenType = "ACCESS",
                        expiresAt = expiresAt,
                        reason = "LOGOUT"
                    )
                )
            }

            // Revoke refresh token
            refreshToken?.let { token ->
                val storedToken = refreshTokenRepository.findByToken(token)
                if (storedToken.isPresent) {
                    val revokedToken = storedToken.get().copy(
                        isRevoked = true,
                        revokedAt = LocalDateTime.now()
                    )
                    refreshTokenRepository.save(revokedToken)
                }
            }
        } catch (e: Exception) {
            // Keep logout idempotent while preserving server-side visibility.
            logger.warn("Error during logout flow", e)
        }
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.substringBefore(",").trim()
        }

        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }

        return request.remoteAddr ?: "unknown"
    }


    fun getPickupRequestsByStatus(status: String): ResponseEntity<String> {
        if (status.isBlank()) {
            return ResponseEntity.badRequest()
                .body(
                    JSONObject()
                        .put("success", false)
                        .put("message", "Status parameter is required")
                        .toString()
                )
        }

        return try {
            val records = sfManager.fetchPickupRequestsByStatus(status)
            if (records == null || records.length() == 0) {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        JSONObject()
                            .put("success", false)
                            .put("message", "No pickup request found for status '$status'")
                            .toString()
                    )
            } else {
                log("getPickupRequestsByStatus - Fetched pickup request for status='$status'")
                ResponseEntity.ok(
                    JSONObject()
                        .put("success", true)
                        .put("message", "Pickup request fetched successfully")
                        .put("data", records)
                        .toString()
                )
            }
        } catch (e: Exception) {
            log("getPickupRequestsByStatus - Exception: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    JSONObject()
                        .put("success", false)
                        .put("message", "Failed to fetch from Salesforce")
                        .toString()
                )
        }
    }

    fun updatePickupDetails(status: String): ResponseEntity<String> {
        if (status.isBlank()) {
            return ResponseEntity.badRequest()
                .body(
                    JSONObject()
                        .put("success", false)
                        .put("message", "Status parameter is required")
                        .toString()
                )
        }

        return try {
            val records = sfManager.fetchPickupRequestsByStatus(status)
            if (records == null || records.length() == 0) {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        JSONObject()
                            .put("success", false)
                            .put("message", "No pickup request found for status '$status'")
                            .toString()
                    )
            } else {
                log("getPickupRequestsByStatus - Fetched pickup request for status='$status'")
                ResponseEntity.ok(
                    JSONObject()
                        .put("success", true)
                        .put("message", "Pickup request fetched successfully")
                        .put("data", records)
                        .toString()
                )
            }
        } catch (e: Exception) {
            log("getPickupRequestsByStatus - Exception: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    JSONObject()
                        .put("success", false)
                        .put("message", "Failed to fetch from Salesforce")
                        .toString()
                )
        }
    }
//    fun updatePickupRequest(request: UpdatePickupRequest): ResponseEntity<ApiResponse<Any>> {
//        return try {
//            val sfResponse = sfManager.updatePickupRequest(request.consignmentId, request.estimatedPickupDate)
//
//            if (sfResponse != null && sfResponse.isSuccess) {
//                ResponseEntity.ok(
//                    ApiResponse.success(
//                        message = "Pickup date updated successfully",
//                        data = null
//                    )
//                )
//            } else {
//                val errorMessage = sfResponse?.message ?: "Unknown error"
//                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(
//                        ApiResponse.failure(
//                            message = "Failed to update Salesforce: $errorMessage"
//                        ) as ApiResponse<Any>
//                    )
//            }
//        } catch (e: Exception) {
//            logger.error("Error updating pickup request", e)
//            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(
//                    ApiResponse.failure(
//                        message = "An error occurred while updating pickup request: ${e.message}"
//                    ) as ApiResponse<Any>
//                )
//        }
//    }
}


