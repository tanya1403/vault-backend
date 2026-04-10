package com.vaultlink.app.security

import com.vaultlink.app.repository.BlacklistedTokenRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val customUserDetailsService: CustomUserDetailsService,
    private val blacklistedTokenRepository: BlacklistedTokenRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        val subject = try {
            jwtService.extractSubject(token)
        } catch (_: Exception) {
            filterChain.doFilter(request, response)
            return
        }

        if (subject.isNullOrBlank() || SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        // Check if token is blacklisted
        val jti = jwtService.extractJti(token)
        if (jti != null && blacklistedTokenRepository.existsByJti(jti)) {
            filterChain.doFilter(request, response)
            return
        }

        val userDetails = customUserDetailsService.loadUserByUsername(subject)
        if (userDetails is AuthenticatedUser && jwtService.isTokenValid(token, userDetails)) {
            val authentication = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities,
            ).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }

            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}
