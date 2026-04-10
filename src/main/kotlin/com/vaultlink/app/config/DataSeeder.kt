package com.vaultlink.app.config

import com.vaultlink.app.model.User
import com.vaultlink.app.repository.UserRepository
import com.vaultlink.app.security.SeedProperties
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DataSeeder {

    @Bean
    @Profile("local", "test")
    fun seedDefaultUser(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        seedProperties: SeedProperties,
    ): CommandLineRunner = CommandLineRunner {
        if (!seedProperties.enabled) {
            return@CommandLineRunner
        }

        val seedUsername = seedProperties.username.trim()
        val seedPassword = seedProperties.password
        if (seedUsername.isBlank() || seedPassword.isBlank()) {
            return@CommandLineRunner
        }

        if (userRepository.existsByUsernameIgnoreCase(seedUsername)) {
            return@CommandLineRunner
        }

        userRepository.save(
            User(
                username = seedUsername,
                passwordHash = passwordEncoder.encode(seedPassword),
                fullName = seedProperties.fullName,
                enabled = true,
                roles = mutableSetOf("ADMIN"),
            )
        )
    }
}
