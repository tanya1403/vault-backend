package com.vaultlink.app.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vault_blacklisted_tokens", indexes = [
    Index(name = "idx_vault_blacklisted_token_jti", columnList = "jti"),
    Index(name = "idx_vault_blacklisted_token_expires_at", columnList = "expires_at")
])
data class BlacklistedToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "jti", unique = true, nullable = false, length = 100)
    val jti: String = "", // JWT ID claim

    @Column(name = "user_id", nullable = false)
    val userId: String = "",

    @Column(name = "token_type", nullable = false, length = 20)
    val tokenType: String = "ACCESS", // ACCESS or REFRESH

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "blacklisted_at", nullable = false)
    val blacklistedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "reason", length = 200)
    val reason: String? = null // LOGOUT, SECURITY, EXPIRED
)
