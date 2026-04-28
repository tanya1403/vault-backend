package com.vaultlink.app.repository

import com.vaultlink.app.model.VaultRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VaultRoleRepository : JpaRepository<VaultRole, String> {
    fun findAllByOrderBySortOrderAsc(): List<VaultRole>
}
