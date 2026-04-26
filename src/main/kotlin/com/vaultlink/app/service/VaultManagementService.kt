package com.vaultlink.app.service

import MarkVaultRequest
import com.vaultlink.app.manager.SalesforceManager
import com.vaultlink.app.model.VaultLaiAcknowledgement
import com.vaultlink.app.repository.VaultLaiAckRepository
import com.vaultlink.app.utills.DateTimeUtils
import com.vaultlink.app.utills.MESSAGE
import com.vaultlink.app.utills.OneResponse
import com.vaultlink.app.utills.SUCCESS
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class VaultManagementService(
    @Autowired val sfManager: SalesforceManager,
    @Autowired val oneResponse: OneResponse,
    @Autowired val vaultLaiAckRepository: VaultLaiAckRepository
) {

    private val PAGE_SIZE = 50

    // ------------------ BRANCH API ------------------

    fun getBranches(search: String?, lastBranch: String?): ResponseEntity<String> {
        return try {
            val (content, cursor, hasMore) = sfManager.getBranches(search, lastBranch, PAGE_SIZE)

            val responseJson = JSONObject()
                .put("content", JSONArray(content))
                .put("nextCursor", cursor)
                .put("hasMore", hasMore)

            oneResponse.getSuccessResponse(responseJson)
        } catch (e: Exception) {
            oneResponse.operationFailedResponse("Failed to fetch branches: ${e.message}")
        }
    }

    fun getLais(branchId: String, search: String?, lastLai: String?): ResponseEntity<String> {
        return try {
            val (content, cursor, hasMore) = sfManager.getLais(branchId, search, lastLai, PAGE_SIZE)

            val responseJson = JSONObject()
                .put("content", JSONArray(content))
                .put("nextCursor", cursor)
                .put("hasMore", hasMore)

            oneResponse.getSuccessResponse(responseJson)
        } catch (e: Exception) {
            oneResponse.operationFailedResponse("Failed to fetch LAIs: ${e.message}")
        }
    }

    fun getDocuments(lai: String, lastCreatedDate: String?): ResponseEntity<String> {
        return try {
            val content = sfManager.getDocuments(lai, lastCreatedDate, PAGE_SIZE)

            val nextCursor =
                if (content.isNotEmpty())
                    content.last().createdDate
                else null

            val responseJson = JSONObject()
                .put("content", JSONArray(content))
                .put("nextCursor", nextCursor)
                .put("hasMore", content.size == PAGE_SIZE)

            oneResponse.getSuccessResponse(responseJson)
        } catch (e: Exception) {
            oneResponse.operationFailedResponse("Failed to fetch documents: ${e.message}")
        }
    }

    fun markDocumentsAsVaulted(
        request: MarkVaultRequest
    ): ResponseEntity<String> {

        if (request.documentIds.isEmpty()) {
            return oneResponse.invalidData("Document Id is required")
        }

        return try {

            val vaultDate = request.vaultingDate
                ?: DateTimeUtils.getCurrentDate()

            val success = sfManager.updateDocumentsVaultingDate(
                request.documentIds,
                vaultDate
            )

            if (!success) {
                return oneResponse.operationFailedResponse("Failed to mark document as vaulted")
            }

            oneResponse.getSuccessResponse(
                JSONObject()
                    .put("success", true)
                    .put("message", "Document Vaulted Successfully.")
            )
        } catch (e: Exception) {
            oneResponse.operationFailedResponse("An unexpected error occurred: ${e.message}")
        }
    }

    fun acknowledgeLais(lais: List<String>) : ResponseEntity<String> {

        if (lais.isEmpty()) {
            return oneResponse.resourceNotFound("LAI list cannot be empty")
        }

        val now = DateTimeUtils.getCurrentDateTimeInIST()

        val records = lais.mapNotNull { lai ->
            if (lai.isBlank()) return@mapNotNull null

            VaultLaiAcknowledgement().apply {
                this.lai = lai
                acknowledgedAt = now
                isNotified = false
            }

        }

        vaultLaiAckRepository.saveAll(records)
        return oneResponse.getSuccessResponse(JSONObject().put(SUCCESS, true).put(MESSAGE, "LAIs acknowledged successfully."))
    }

}