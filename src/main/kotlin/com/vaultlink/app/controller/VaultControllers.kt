package com.vaultlink.app.controller

import MarkVaultRequest
import com.vaultlink.app.dto.LoginRequest
import com.vaultlink.app.dto.RefreshTokenRequest
import com.vaultlink.app.service.VaultManagementService

import com.vaultlink.app.service.VaultService
import com.vaultlink.app.utills.OneResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/vault/v1")
class VaultController(
    private val vaultService: VaultService,
    private val vaultManagementService: VaultManagementService,
    @Autowired val oneResponse: OneResponse
) {

    @PostMapping(
        "/login",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<String> {
        return try{
            vaultService.login(request)
        }catch(e:Exception){
            oneResponse.defaultFailureResponse
        }
    }

    @PostMapping(
        "/refresh",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<String> {
        return try{
            vaultService.refreshToken(request)
        }catch(e:Exception){
            oneResponse.defaultFailureResponse
        }
    }

    @PostMapping(
        "/logout",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun logout(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody(required = false) request: com.vaultlink.app.dto.LogoutRequest? = com.vaultlink.app.dto.LogoutRequest(),
        httpRequest: HttpServletRequest
    ): ResponseEntity<String> {
        return try {
            val token = if (authorization.startsWith("Bearer ")) {
                authorization.substring(7)
            } else {
                authorization
            }
            vaultService.logout(token, request?.refreshToken)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }

    @GetMapping("/pickup-requests")
    fun fetchPickupRequestsByStatus(
        @RequestParam(required = true) status: String
    ): ResponseEntity<String> {
        return try {
            vaultService.getPickupRequestsByStatus(status)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }

    @PostMapping(
        "/update-pickup-date",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updatePickupDate(
        @Valid @RequestBody request: com.vaultlink.app.dto.UpdatePickupRequest
    ): ResponseEntity<String> {
        return try {
            vaultService.updatePickupRequest(request)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }


    @GetMapping("/branches")
    fun getBranches(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) lastBranch: String?
    ): ResponseEntity<String> {
        return try {
            vaultManagementService.getBranches(search, lastBranch)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }

    @GetMapping("/lais")
    fun getLais(
        @RequestParam branchId: String,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) lastLai: String?
    ): ResponseEntity<String> {
        return try {
            vaultManagementService.getLais(branchId, search, lastLai)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }

    @GetMapping("/documents")
    fun getDocuments(
        @RequestParam lai: String,
        @RequestParam(required = false) lastCreatedDate: String?
    ): ResponseEntity<String> {
        return try {
            vaultManagementService.getDocuments(lai, lastCreatedDate)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }

    @PostMapping("/vault/document/mark-vaulted")
    fun markVaulted(
        @RequestBody request: MarkVaultRequest
    ): ResponseEntity<String> {
        return try {
            vaultManagementService.markDocumentsAsVaulted(request)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }

    @PostMapping("/vault/lai/acknowledge")
    fun acknowledgeLais(@RequestBody request: Map<String, List<String>>): ResponseEntity<String> {

        val lais = request["lais"] ?: return oneResponse.resourceNotFound("LAIs are required")

        return try {
            vaultManagementService.acknowledgeLais(lais)
        } catch (e: Exception) {
            oneResponse.defaultFailureResponse
        }
    }
}
