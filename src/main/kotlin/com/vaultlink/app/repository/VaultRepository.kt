package com.vaultlink.app.repository

import com.vaultlink.app.model.VaultLaiAcknowledgement
import org.springframework.data.jpa.repository.JpaRepository

interface VaultLaiAckRepository : JpaRepository<VaultLaiAcknowledgement, String> {

    fun findTopByLaiOrderByAcknowledgedAtDesc(lai: String): VaultLaiAcknowledgement?

    fun findByIsNotifiedFalse(): List<VaultLaiAcknowledgement>
}