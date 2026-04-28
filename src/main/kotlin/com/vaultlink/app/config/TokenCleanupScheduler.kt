package com.vaultlink.app.config

import com.vaultlink.app.repository.BlacklistedTokenRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class TokenCleanupScheduler(
    private val blacklistedTokenRepository: BlacklistedTokenRepository
) {

    // Run every hour to clean up expired blacklisted access tokens
    @Scheduled(fixedRate = 3600000)
    @Transactional
    fun cleanupExpiredTokens() {
        val now = LocalDateTime.now()
        blacklistedTokenRepository.deleteByExpiresAtBefore(now)
        println("Blacklisted token cleanup completed at $now")
    }

    // Run daily at 2 AM to clean up tokens older than 7 days
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    fun dailyCleanup() {
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)
        blacklistedTokenRepository.deleteByExpiresAtBefore(sevenDaysAgo)
        println("Daily token cleanup completed at $sevenDaysAgo")
    }
}
