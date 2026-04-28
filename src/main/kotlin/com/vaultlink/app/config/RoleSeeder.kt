package com.vaultlink.app.config

import com.vaultlink.app.model.VaultRole
import com.vaultlink.app.repository.VaultRoleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Seeds the [vault_roles] table with the default operational roles on
 * first startup. Does nothing if rows already exist.
 */
@Component
class RoleSeeder(
    private val roleRepository: VaultRoleRepository
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(RoleSeeder::class.java)

    override fun run(args: ApplicationArguments) {
        if (roleRepository.count() > 0) {
            logger.info("vault_roles already seeded — skipping.")
            return
        }

        val roles = listOf(
            VaultRole(
                roleKey          = "CSM",
                displayName      = "Customer Success Manager",
                description      = "View-only access to branch pickups and document statuses.",
                permPickupView   = true,
                permSchedulePickup  = false,
                permVaultView    = true,
                permTransitTracking = false,
                permCreateUser   = false,
                permSystemAdmin  = false,
                sortOrder        = 1
            ),
            VaultRole(
                roleKey          = "BM",
                displayName      = "Branch Manager",
                description      = "Manage branch-level pickup requests and scheduling.",
                permPickupView   = true,
                permSchedulePickup  = true,
                permVaultView    = true,
                permTransitTracking = false,
                permCreateUser   = false,
                permSystemAdmin  = false,
                sortOrder        = 2
            ),
            VaultRole(
                roleKey          = "ADMIN",
                displayName      = "Administrator",
                description      = "Full system access including user management and global configurations.",
                permPickupView   = true,
                permSchedulePickup  = true,
                permVaultView    = true,
                permTransitTracking = true,
                permCreateUser   = true,
                permSystemAdmin  = false,
                sortOrder        = 3
            ),
            VaultRole(
                roleKey          = "OPS",
                displayName      = "Operations",
                description      = "Operational fulfillment, transit tracking and delivered status management.",
                permPickupView   = true,
                permSchedulePickup  = false,
                permVaultView    = true,
                permTransitTracking = true,
                permCreateUser   = false,
                permSystemAdmin  = false,
                sortOrder        = 4
            ),
            VaultRole(
                roleKey          = "KLEETO",
                displayName      = "Kleeto Admin",
                description      = "Kleeto internal administrative access for vault infrastructure and system-wide configurations.",
                permPickupView   = true,
                permSchedulePickup  = true,
                permVaultView    = true,
                permTransitTracking = true,
                permCreateUser   = true,
                permSystemAdmin  = true,
                sortOrder        = 5
            )
        )

        roleRepository.saveAll(roles)
        logger.info("vault_roles seeded with ${roles.size} roles.")
    }
}
