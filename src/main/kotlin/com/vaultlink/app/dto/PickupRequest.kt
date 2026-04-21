package com.vaultlink.app.dto

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
    val actualPickupDate: String? = null
)
