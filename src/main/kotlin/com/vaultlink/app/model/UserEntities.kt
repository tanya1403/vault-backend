package com.vaultlink.app.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "vault_users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,
    @Column(nullable = false, unique = true, length = 150)
    var username: String = "",
    @Column(nullable = false, length = 255)
    var passwordHash: String = "",
    @Column(nullable = false, length = 150)
    var fullName: String = "",
    @Column(nullable = false)
    var enabled: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,
    @Column(name = "refresh_token_last_used_at")
    var refreshTokenLastUsedAt: Instant? = null,
) {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vault_user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role_name", nullable = false, length = 50)
    var roles: MutableSet<String> = mutableSetOf()

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "vault_login_audits")
class LoginAudit(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @Column(name = "logged_in_at", nullable = false, updatable = false)
    var loggedInAt: Instant = Instant.now(),
    @Column(name = "ip_address", length = 64)
    var ipAddress: String? = null,
    @Column(name = "user_agent", length = 512)
    var userAgent: String? = null,
)
