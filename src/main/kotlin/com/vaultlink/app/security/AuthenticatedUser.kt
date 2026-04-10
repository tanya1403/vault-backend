package com.vaultlink.app.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class AuthenticatedUser(
    val id: String,
    username: String,
    password: String,
    enabled: Boolean,
    authorities: Collection<GrantedAuthority>,
) : User(username, password, enabled, true, true, true, authorities)
