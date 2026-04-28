package com.vaultlink.app.dto

import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile

data class UpdatePickupRequest(
    @field:NotBlank(message = "Record ID is required")
    val recordId: String? = null,
    val consignmentId: String? = null,
    val pod: String? = null,
    val status: String? = null,
    val actualPickupDate: String? = null,
    val estimatedPickupDate: String? = null,
    val deliveryDate: String? = null,
    val remarks: String? = null,
    val file :MultipartFile?=null
)

data class PickupRequest(
    val id: String,
    val branchName: String,
    val branchAddress: String,
    val csmBM: String,
    val mobile: String,
    val noOfFiles: Int,
    val noOfBoxes: Int,
    val requestedDate: String,
    val consignmentId: String,
    val ownerName: String,
    val status: String,
    val ownerEmail: String? = null,
    val expectedPickupDate: String? = null,
    val actualPickupDate: String? = null
)
