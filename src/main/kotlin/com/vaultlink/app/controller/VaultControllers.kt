package com.vaultlink.app.controller

import BranchDTO
import CursorResponse
import DocumentDTO
import LaiDTO
import MarkVaultRequest
import ResponseDto
import com.vaultlink.app.dto.ApiResponse
import com.vaultlink.app.dto.LoginRequest
import com.vaultlink.app.dto.LoginResponse
import com.vaultlink.app.dto.LogoutRequest
import com.vaultlink.app.dto.LogoutResponse
import com.vaultlink.app.dto.LoginResponse
import com.vaultlink.app.dto.RefreshTokenRequest
import com.vaultlink.app.dto.RefreshTokenResponse
import com.vaultlink.app.dto.UpdatePickupRequest
import com.vaultlink.app.service.VaultManagementService

import com.vaultlink.app.service.VaultService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.json.JSONObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/vault/v1")
class VaultController(
    private val vaultService: VaultService,
    private val vaultManagementService: VaultManagementService
) {

    @PostMapping(
        "/login",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
//        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<LoginResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(
                message = "Login successful",
//                data = vaultService.login(request, httpRequest),
                data = vaultService.login(request),
            )
        )

    @PostMapping(
        "/refresh",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<RefreshTokenResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(
                message = "Token refreshed successfully",
                data = vaultService.refreshToken(request),
            )
        )

    @PostMapping(
        "/logout",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun logout(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody(required = false) request: LogoutRequest? = LogoutRequest(),
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<LogoutResponse>> {
        val token = if (authorization.startsWith("Bearer ")) {
            authorization.substring(7)
        } else {
            authorization
        }

        vaultService.logout(token, request?.refreshToken)

        return ResponseEntity.ok(
            ApiResponse.success(
                message = "Logged out successfully",
                data = LogoutResponse(message = "Logged out successfully"),
            )
        )
    }

    @GetMapping("/pickup-requests")
    fun fetchPickupRequestsByStatus(
        @RequestParam(required = true) status: String
    ): ResponseEntity<ApiResponse<List<PickupRequest>>> =
        vaultService.getPickupRequestsByStatus(status)

    @PostMapping(
        "/update-pickup-date",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updatePickupDate(
        @Valid @RequestBody request: UpdatePickupRequest
    ): ResponseEntity<Any> =
        vaultService.updatePickupRequest(request)


//    @PostMapping("/pickup-requests")
//    fun updatePickUpDetails(
//        @RequestParam(required = true) status: String
//    ): ResponseEntity<String> =
//        vaultService.getPickupRequestsByStatus(status)
    @PostMapping("/pickup-requests")
    fun updatePickUpDetails(
        @RequestParam(required = true) status: String
    ): ResponseEntity<String> =
        vaultService.getPickupRequestsByStatus(status)


    @GetMapping("/branches")
    fun getBranches(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) lastBranch: String?
    ): ResponseEntity<CursorResponse<BranchDTO>> {
        return ResponseEntity.ok(vaultManagementService.getBranches(search, lastBranch))
    }

    @GetMapping("/lais")
    fun getLais(
        @RequestParam branchId: String,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) lastLai: String?
    ): ResponseEntity<CursorResponse<LaiDTO>> {
        return ResponseEntity.ok(vaultManagementService.getLais(branchId, search, lastLai))
    }

    @GetMapping("/documents")
    fun getDocuments(
        @RequestParam lai: String,
        @RequestParam(required = false) lastCreatedDate: String?
    ): ResponseEntity<CursorResponse<DocumentDTO>> {
        val response = vaultManagementService.getDocuments(lai, lastCreatedDate)
        return ResponseEntity.ok(response)
    }
    @PostMapping("/vault/document/mark-vaulted")
    fun markVaulted(
        @RequestBody request: MarkVaultRequest
    ): ResponseEntity<ResponseDto> {
        return ResponseEntity.ok(vaultManagementService.markDocumentAsVaulted(request))
    }
}
