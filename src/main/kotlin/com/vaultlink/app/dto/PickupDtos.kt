package com.vaultlink.app.dto

import jakarta.validation.constraints.NotBlank

data class UpdatePickupRequest(
    @field:NotBlank(message = "Record ID is required")
    val recordId: String? = null,

    val consignmentId: String? = null,

    val pod: String? = null,

    val status: String? = null,

    val actualPickupDate: String? = null,

    val estimatedPickupDate: String? = null,

    val deliveryDate: String? = null
)
