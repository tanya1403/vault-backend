package com.vaultlink.app.security

import com.vaultlink.app.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
) {

    val expirationSeconds: Long
        get() = jwtProperties.expirationMs / 1000

    val refreshExpirationSeconds: Long
        get() = jwtProperties.refreshExpirationMs / 1000

    fun generateToken(user: User): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.expirationMs)
        val jti = UUID.randomUUID().toString()

        return Jwts.builder()
            .setSubject(user.username)
            .setId(jti) // JWT ID for blacklisting
            .claim("username", user.username)
            .claim("roles", user.roles.toList())
            .claim("userId", user.id)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(signingKey())
            .compact()
    }

    fun generateRefreshToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    fun extractSubject(token: String): String? =
        parseClaims(token).subject

    fun extractJti(token: String): String? =
        parseClaims(token).id

    fun extractUserId(token: String): String? =
        parseClaims(token).get("userId", String::class.java)

    fun extractRoles(token: String): List<String> =
        parseClaims(token).get("roles", List::class.java) as List<String>

    fun isTokenValid(token: String, user: AuthenticatedUser): Boolean {
        val claims = parseClaims(token)
        return claims.subject == user.username && claims.expiration.after(Date())
    }

    fun isTokenExpired(token: String): Boolean {
        return parseClaims(token).expiration.before(Date())
    }

    private fun parseClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(signingKey())
            .build()
            .parseClaimsJws(token)
            .body

    private fun signingKey(): SecretKey =
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
}
