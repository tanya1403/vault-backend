package com.vaultlink.app.repository

import com.vaultlink.app.model.RefreshToken
import com.vaultlink.app.model.BlacklistedToken
import com.vaultlink.app.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun findByUser(user: User): List<RefreshToken>
    fun findByUserAndIsRevokedFalse(user: User): Optional<RefreshToken>
    fun deleteByUser(user: User)
    fun deleteByExpiresAtBefore(date: LocalDateTime)
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :now WHERE rt.user.id = :userId")
    fun revokeAllUserTokens(@Param("userId") userId: String, @Param("now") now: LocalDateTime)
}

@Repository
interface BlacklistedTokenRepository : JpaRepository<BlacklistedToken, Long> {
    fun findByJti(jti: String): Optional<BlacklistedToken>
    fun deleteByExpiresAtBefore(date: LocalDateTime)
    fun existsByJti(jti: String): Boolean
}
