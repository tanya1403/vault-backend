package com.vaultlink.app.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Stores all available operational roles and their permission flags.
 * The [roleKey] (e.g. "CSM", "BM", "ADMIN") is the primary key and
 * matches the values stored in [vault_user_roles].role_name.
 */
@Entity
@Table(name = "vault_roles")
class VaultRole(

    @Id
    @Column(name = "role_key", length = 30, nullable = false)
    var roleKey: String = "",

    @Column(name = "display_name", length = 60, nullable = false)
    var displayName: String = "",

    @Column(name = "description", length = 300, nullable = false)
    var description: String = "",

    // ----- permission flags -----

    @Column(name = "perm_pickup_view", nullable = false)
    var permPickupView: Boolean = false,

    @Column(name = "perm_schedule_pickup", nullable = false)
    var permSchedulePickup: Boolean = false,

    @Column(name = "perm_vault_view", nullable = false)
    var permVaultView: Boolean = false,

    @Column(name = "perm_transit_tracking", nullable = false)
    var permTransitTracking: Boolean = false,

    @Column(name = "perm_create_user", nullable = false)
    var permCreateUser: Boolean = false,

    @Column(name = "perm_system_admin", nullable = false)
    var permSystemAdmin: Boolean = false,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
)
