package com.vaultlink.app.model

import com.vaultlink.app.utills.DateTimeUtils
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "vault_lai_acknowledgement")
class VaultLaiAcknowledgement {

    @Id
    @Column(updatable = false, nullable = false)
    var id: String = UUID.randomUUID().toString()

    var lai: String = ""

    var acknowledgedAt: String = DateTimeUtils.getCurrentDateTimeInIST()

    var isNotified: Boolean = false

    var createDatetime: String? = DateTimeUtils.getCurrentDateTimeInIST()

    var updateDatetime: String? = DateTimeUtils.getCurrentDateTimeInIST()
}
