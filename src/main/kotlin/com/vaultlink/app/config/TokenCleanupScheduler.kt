package com.vaultlink.app.config

import com.vaultlink.app.repository.RefreshTokenRepository
import com.vaultlink.app.repository.BlacklistedTokenRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class TokenCleanupScheduler(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val blacklistedTokenRepository: BlacklistedTokenRepository
) {

    // Run every hour to clean up expired tokens
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    fun cleanupExpiredTokens() {
        val now = LocalDateTime.now()
        
        // Clean up expired refresh tokens
        refreshTokenRepository.deleteByExpiresAtBefore(now)
        
        // Clean up expired blacklisted tokens
        blacklistedTokenRepository.deleteByExpiresAtBefore(now)
        
        println("Token cleanup completed at $now")
    }

    // Run daily at 2 AM to clean up old data
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    @Transactional
    fun dailyCleanup() {
        val now = LocalDateTime.now()
        val sevenDaysAgo = now.minusDays(7)
        
        // Clean up tokens older than 7 days (even if not expired)
        refreshTokenRepository.deleteByExpiresAtBefore(sevenDaysAgo)
        
        // Clean up blacklisted tokens older than 7 days
        blacklistedTokenRepository.deleteByExpiresAtBefore(sevenDaysAgo)
        
        println("Daily token cleanup completed at $now")
    }
}
