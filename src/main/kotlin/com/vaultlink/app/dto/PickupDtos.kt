package com.vaultlink.app.dto

import jakarta.validation.constraints.NotBlank

data class UpdatePickupRequest(
    @field:NotBlank(message = "Consignment ID is required")
    val consignmentId: String,

    @field:NotBlank(message = "Estimated pickup date is required")
    val estimatedPickupDate: String
)
