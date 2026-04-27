package com.vaultlink.app.service

import MarkVaultRequest
import com.vaultlink.app.helper.MailHelper
import com.vaultlink.app.manager.SalesforceManager
import com.vaultlink.app.model.VaultLaiAcknowledgement
import com.vaultlink.app.repository.VaultLaiAckRepository
import com.vaultlink.app.utills.DateTimeUtils
import com.vaultlink.app.utills.KAINAAT_EMAIL_ID
import com.vaultlink.app.utills.LoggerUtils
import com.vaultlink.app.utills.MESSAGE
import com.vaultlink.app.utills.OneResponse
import com.vaultlink.app.utills.RANAN_EMAIL_ID
import com.vaultlink.app.utills.SANJAY_EMAIL_ID
import com.vaultlink.app.utills.SUCCESS
import com.vaultlink.app.utills.TANYA_EMAIL_ID
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class VaultManagementService(
    @Autowired val sfManager: SalesforceManager,
    @Autowired val oneResponse: OneResponse,
    @Autowired val vaultLaiAckRepository: VaultLaiAckRepository,
    @Autowired val mailHelper: MailHelper
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

        val now = DateTimeUtils.getCurrentDate()

        val records = lais.mapNotNull { lai ->
            if (lai.isBlank()) return@mapNotNull null

            VaultLaiAcknowledgement().apply {
                this.lai = lai
                acknowledgedAt = now
                isNotified = false
            }

        }

        vaultLaiAckRepository.saveAll(records)

        sendAcknowledgementMailAsync(records, now)

        return oneResponse.getSuccessResponse(JSONObject().put(SUCCESS, true).put(MESSAGE, "LAIs acknowledged successfully."))
    }

    @Async
    fun sendAcknowledgementMailAsync(
        records: List<VaultLaiAcknowledgement>,
        acknowledgedAt: String
    ) {
        try {
            val totalAcknowledged = records.size

            val sb = StringBuilder()
            sb.append("Please find the LAI acknowledgement details below:")
            sb.append("\n\nTotal LAIs Acknowledged : $totalAcknowledged")
            sb.append("\nAcknowledged At : $acknowledgedAt")
            sb.append("\n\nAcknowledged LAIs:")

            records.forEachIndexed { index, record ->
                sb.append("\n${index + 1}. ${record.lai}")
            }

            sb.append("\n\n\nThis is an auto generated email. Please do not reply.")
            sb.append("\n- Homefirst")

            mailHelper.sendMimeMessage(
                arrayOf("Kainaat.zaidi@homefirstindia.com"),
                "Vault Management - LAI Acknowledgement",
                sb.toString(),
                cc = arrayOf(
                    KAINAAT_EMAIL_ID
                    // RANAN_EMAIL_ID, SANJAY_EMAIL_ID, TANYA_EMAIL_ID
                )
            )

        } catch (e: Exception) {
            LoggerUtils.log("Failed to send acknowledgement mail: ${e.message}")
        }
    }

}