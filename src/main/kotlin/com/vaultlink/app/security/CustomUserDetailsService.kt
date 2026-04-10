package com.vaultlink.app.security

import com.vaultlink.app.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsernameIgnoreCase(username)
            ?: throw UsernameNotFoundException("User not found")

        return AuthenticatedUser(
            id = user.id ?: throw UsernameNotFoundException("User id not found"),
            username = user.username,
            password = user.passwordHash,
            enabled = user.enabled,
            authorities = user.roles
                .map { role -> role.uppercase().removePrefix("ROLE_") }
                .map { role -> SimpleGrantedAuthority("ROLE_$role") },
        )
    }
}
