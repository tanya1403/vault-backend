package com.vaultlink.app.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vault_refresh_tokens", indexes = [
    Index(name = "idx_vault_refresh_token_user_id", columnList = "user_id"),
    Index(name = "idx_vault_refresh_token_token", columnList = "token"),
    Index(name = "idx_vault_refresh_token_expires_at", columnList = "expires_at")
])
data class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 500)
    val token: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now().plusHours(48),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_revoked", nullable = false)
    val isRevoked: Boolean = false,

    @Column(name = "revoked_at")
    val revokedAt: LocalDateTime? = null,

    @Column(name = "last_used_at")
    val lastUsedAt: LocalDateTime? = null
)
