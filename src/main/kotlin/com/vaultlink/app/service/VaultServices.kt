package com.vaultlink.app.service

import com.vaultlink.app.dto.ApiResponse
import com.vaultlink.app.dto.LoginRequest
import com.vaultlink.app.dto.LoginResponse
import com.vaultlink.app.dto.PickupRequest
import com.vaultlink.app.dto.RefreshTokenRequest
import com.vaultlink.app.dto.RefreshTokenResponse
import com.vaultlink.app.dto.UpdatePickupRequest

import com.vaultlink.app.manager.SalesforceManager
import com.vaultlink.app.model.User
import com.vaultlink.app.repository.UserRepository
import com.vaultlink.app.repository.BlacklistedTokenRepository
import com.vaultlink.app.security.JwtService
import com.vaultlink.app.dto.RegisterRequest
import com.vaultlink.app.model.BlacklistedToken
import com.vaultlink.app.utills.STATUS
import com.vaultlink.app.utills.SUCCESS
import com.vaultlink.app.utills.MESSAGE
import com.vaultlink.app.utills.OneResponse
import com.vaultlink.app.utills.LoggerUtils.log
import jakarta.servlet.http.HttpServletRequest
import org.json.JSONArray
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
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

@Service
class VaultService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val oneResponse: OneResponse,
    private val blacklistedTokenRepository: BlacklistedTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder,
    @Autowired val sfManager: SalesforceManager,
    @Autowired val emailService: EmailService
) {

    private val logger = LoggerFactory.getLogger(VaultService::class.java)

//    @Transactional
//    fun login(
//        request: LoginRequest,
////        httpRequest: HttpServletRequest,
//    ): LoginResponse {
//        val normalizedUsername = request.username.trim()
//
//        val authentication = try {
//            authenticationManager.authenticate(
//                UsernamePasswordAuthenticationToken(normalizedUsername, request.password)
//            )
//        } catch (_: DisabledException) {
//            throw BadCredentialsException("Invalid username or password")
//        } catch (_: BadCredentialsException) {
//            throw BadCredentialsException("Invalid username or password")
//        }
//
//        if (!authentication.isAuthenticated) {
//            throw BadCredentialsException("Invalid username or password")
//        }
//
//        val user = userRepository.findByUsernameIgnoreCase(normalizedUsername)
//            ?: throw BadCredentialsException("Invalid username or password")
//
//        if (!user.enabled) {
//            throw BadCredentialsException("Invalid username or password")
//        }
//
//        user.lastLoginAt = Instant.now()
//        userRepository.save(user)
//
////        loginAuditRepository.save(
////            LoginAudit(
////                user = user,
////                ipAddress = extractClientIp(httpRequest),
////                userAgent = httpRequest.getHeader("User-Agent"),
////            )
////        )
//
//        // Revoke all existing refresh tokens for this user
//        refreshTokenRepository.revokeAllUserTokens(user.id!!, LocalDateTime.now())
//
//        // Generate new tokens
//        val accessToken = jwtService.generateToken(user)
//        val refreshToken = jwtService.generateRefreshToken()
//        // Store refresh token in database
//        val refreshTokenEntity = RefreshToken(
//            token = refreshToken,
//            user = user,
//            expiresAt = LocalDateTime.now().plusSeconds(jwtService.refreshExpirationSeconds),
//            createdAt = LocalDateTime.now()
//        )
//        refreshTokenRepository.save(refreshTokenEntity)
//
//        return LoginResponse(
//            accessToken = accessToken,
//            refreshToken = refreshToken,
//            expiresIn = jwtService.expirationSeconds,
//            user = com.vaultlink.app.dto.AuthenticatedUserResponse(
//                id = user.id!!,
//                username = user.username,
//                fullName = user.fullName,
//                roles = user.roles.toList(),
//            ),
//        )
//    }
    
    @Transactional
    fun register(request: RegisterRequest): ResponseEntity<String> {
        val normalizedUsername = request.username.trim()

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            return oneResponse.duplicateRecord("Username already exists")
        }

        logger.info("Registering user: $normalizedUsername with role: ${request.role}")

        val user = User(
            username = normalizedUsername,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName.trim(),
            enabled = true,
        )
        user.roles.add(request.role.uppercase())

        userRepository.save(user)

        return oneResponse.getSuccessResponse(
            JSONObject()
                .put(STATUS, SUCCESS)
                .put(MESSAGE, "User created successfully")
                .put("userId", user.id)
        )
    }

    @Transactional
    fun login(
        request: LoginRequest,
//        httpRequest: HttpServletRequest,
    ): ResponseEntity<String> {
        val normalizedUsername = request.username.trim()

        val authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(normalizedUsername, request.password)
            )
        } catch (_: DisabledException) {
            throw BadCredentialsException("Invalid username or password")
        } catch (_: BadCredentialsException) {
            throw BadCredentialsException("Invalid username or password")
        }

        if (!authentication.isAuthenticated) {
            throw BadCredentialsException("Invalid username or password")
        }

        val user = userRepository.findByUsernameIgnoreCase(normalizedUsername)
            ?: throw BadCredentialsException("Invalid username or password")

        if (!user.enabled) {
            throw BadCredentialsException("Invalid username or password")
        }

        // Generate tokens
        val accessToken = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken()

        // Store refresh token directly on the user record
        user.lastLoginAt = Instant.now()
        user.refreshToken = refreshToken
        user.refreshTokenExpiresAt = LocalDateTime.now().plusSeconds(jwtService.refreshExpirationSeconds)
        userRepository.save(user)

        return oneResponse.getSuccessResponse(JSONObject()
            .put("accessToken", accessToken)
            .put("refreshToken", refreshToken)
            .put("expiresIn", jwtService.expirationSeconds)
            .put("user", JSONObject()
                .put("id", user.id)
                .put("username", user.username)
                .put("fullName", user.fullName)
                .put("roles", JSONArray(user.roles))
            )
        )
    }

    @Transactional
    fun refreshToken(request: RefreshTokenRequest): ResponseEntity<String> {
        logger.debug("Attempting to refresh token: ${request.refreshToken.take(8)}...")

        val user = userRepository.findByRefreshToken(request.refreshToken)
            ?: run {
                logger.debug("Refresh token not found")
                throw BadCredentialsException("Invalid refresh token")
            }

        if (user.refreshTokenExpiresAt?.isBefore(LocalDateTime.now()) != false) {
            logger.debug("Refresh token is expired for user: ${user.username}")
            throw BadCredentialsException("Session expired: refresh token expired")
        }

        if (!user.enabled) {
            logger.debug("User ${user.username} is disabled")
            throw BadCredentialsException("User account is disabled")
        }

        val newAccessToken = jwtService.generateToken(user)
        logger.debug("New access token issued for: ${user.username}")

        return oneResponse.getSuccessResponse(
            JSONObject()
                .put("accessToken", newAccessToken)
                .put("expiresIn", jwtService.expirationSeconds)
                .put("tokenType", "Bearer")
        )
    }

    @Transactional
    fun logout(accessToken: String, refreshToken: String?): ResponseEntity<String> {
        try {
            val jti = jwtService.extractJti(accessToken)
            val userId = jwtService.extractUserId(accessToken)
            val expiresAt = LocalDateTime.now().plusSeconds(jwtService.expirationSeconds)

            // Blacklist the access token
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

            // Clear the refresh token from the user record
            refreshToken?.let { token ->
                val user = userRepository.findByRefreshToken(token)
                if (user != null) {
                    user.refreshToken = null
                    user.refreshTokenExpiresAt = null
                    userRepository.save(user)
                }
            }
        } catch (e: Exception) {
            logger.warn("Error during logout flow", e)
            return oneResponse.defaultFailureResponse
        }
        return oneResponse.getSuccessResponse(JSONObject().put(MESSAGE, "Logout successful"))
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
            return oneResponse.invalidData("Status parameter is required")
        }

        return try {
            val records = sfManager.fetchPickupRequestsByStatus(status)
            if (records.isNullOrEmpty()) {
                oneResponse.getFailureResponse(
                    JSONObject()
                        .put("success", true)
                        .put("message", "No pickup request found for status '$status'")
                        .put("data", JSONArray())
                )
            } else {
                oneResponse.getSuccessResponse(
                    JSONObject()
                        .put("success", true)
                        .put("message", "Pickup requests fetched successfully")
                        .put("data", JSONArray(records))
                )
            }
        } catch (e: Exception) {
            log("getPickupRequestsByStatus - Exception: ${e.message}")
            oneResponse.operationFailedResponse("Failed to fetch from Salesforce: ${e.message}")
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
            if (records == null || records.isEmpty()) {
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

    fun updatePickupRequest(request: UpdatePickupRequest?): ResponseEntity<String> {
        if (request == null) {
            logger.error("updatePickupRequest - Received null request body")
            return oneResponse.invalidData("Request body is missing")
        }

        return try {
            log("updatePickupRequest - Processing update for recordId: ${request.recordId}, status: ${request.status}")

            // Check if manager is properly injected
            if (sfManager == null) {
                logger.error("updatePickupRequest - sfManager is null (injection failure)")
                return oneResponse.operationFailedResponse("Internal service error: Salesforce integration unavailable")
            }

            // Fetch current state before update to check for status transitions
            val currentPickup = request.recordId?.let { sfManager.fetchPickupRequestById(it) }
            val wasAlreadyScheduled = currentPickup?.status == "Scheduled" || currentPickup?.status == "Pickup scheduled"

            // Using positional arguments to avoid CGLIB/Named parameter conflicts in Kotlin
            val success = sfManager.updatePickupRequest(
                request.recordId,
                request.estimatedPickupDate,
                request.pod,
                request.status,
                request.actualPickupDate,
                request.deliveryDate
            )

            if (success) {
                val isNowScheduled = request.status == "Scheduled" || request.status == "Pickup scheduled" ||
                                    (request.status.isNullOrBlank() && !request.estimatedPickupDate.isNullOrBlank())
                
                if (isNowScheduled && !wasAlreadyScheduled) {
                    try {
                        request.recordId?.let { recordId ->
                            // Fetch updated record to get latest details (like Expected Pickup Date)
                            val updatedPickup = sfManager.fetchPickupRequestById(recordId)
                            val ownerEmail = updatedPickup?.ownerEmail ?: updatedPickup?.csmBM?.takeIf { it.contains("@") }

                            if (updatedPickup != null && !ownerEmail.isNullOrBlank()) {
                                emailService.sendPickupScheduledEmail(ownerEmail, updatedPickup)
                            } else {
                                logger.warn("Could not send email: Owner email not found for recordId: $recordId")
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error triggering email notification", e)
                    }
                }

                // If a file is provided, upload it asynchronously
                if (request.file != null && !request.file.isEmpty && !request.recordId.isNullOrBlank()) {
                    try {
                        val base64 = Base64.getEncoder().encodeToString(request.file.bytes)
                        val fileName = request.file.originalFilename ?: "pickup_document"
                        sfManager.uploadContentDocumentOnSF(base64, fileName, request.recordId)
                    } catch (e: Exception) {
                        logger.error("Failed to initiate file upload after pickup update", e)
                    }
                }

                oneResponse.getSuccessResponse(
                    JSONObject()
                        .put("success", true)
                        .put("message", "Pickup updated successfully")
                )
            } else {
                oneResponse.operationFailedResponse("Failed to update Salesforce record")
            }
        } catch (e: Exception) {
            logger.error("Error updating pickup request", e)
            oneResponse.operationFailedResponse("An error occurred while updating pickup request: ${e.message}")
        }
    }
}


