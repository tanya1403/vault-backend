package com.vaultlink.app.repository

import com.vaultlink.app.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByUsernameIgnoreCase(username: String): User?
    fun existsByUsernameIgnoreCase(username: String): Boolean
    fun findByRefreshToken(token: String): User?
}
