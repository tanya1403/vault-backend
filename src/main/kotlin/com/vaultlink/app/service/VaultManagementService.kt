package com.vaultlink.app.service

import BranchDTO
import CursorResponse
import DocumentDTO
import LaiDTO
import MarkVaultRequest
import ResponseDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.vaultlink.app.networking.SFConnection
import com.vaultlink.app.utills.CommonHelper
import com.vaultlink.app.utills.DateTimeUtils
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VaultManagementService(
    @Autowired val commonHelper: CommonHelper,
    @Autowired val sfConnection: SFConnection
) {

    private val FETCH_BATCH_SIZE = 200
    private val PAGE_SIZE = 50

    // ------------------ BRANCH API ------------------

    fun getBranches(search: String?, lastBranch: String?): CursorResponse<BranchDTO> {

        val branchMap = LinkedHashMap<String, BranchDTO>()
        var cursor = lastBranch
        var hasMore = true

        while (branchMap.size < PAGE_SIZE && hasMore) {

            val query = StringBuilder("""
            SELECT 
                Branch_Name__r.Name,
                Branch_Name__c,
                Branch_Name__r.Branch_Address_line_1__c,
                No_Of_Files__c,
                Owner.Phone, Owner.Name
            FROM Documents_Pickup__c
            WHERE Pickup_Stage__c != 'Delivered'
            AND Pickup_Stage__c != 'Cancelled'
        """.trimIndent())

            if (!search.isNullOrBlank()) {
                query.append(" AND Branch_Name__r.Name LIKE '${commonHelper.escape(search)}%' ")
            }

            if (!cursor.isNullOrBlank()) {
                query.append(" AND Branch_Name__r.Name > '$cursor' ")
            }

            query.append(" ORDER BY Branch_Name__r.Name ASC LIMIT $FETCH_BATCH_SIZE")

            val records = commonHelper.executeQuery(query.toString()) ?: break

            for (i in 0 until records.length()) {

                val rec = records.getJSONObject(i)

                val branchObj = rec.optJSONObject("Branch_Name__r") ?: continue
                val branchName = branchObj.optString("Name")
                val branchId = rec.optString("Branch_Name__c")

                if (branchName.isBlank()) continue

                val address = branchObj.optString("Branch_Address_line_1__c")

                val owner = rec.optJSONObject("Owner")
                val csmName = owner?.optString("Name")
                val mobile = owner?.optString("MobilePhone")

                val docCount = rec.optInt("No_Of_Files__c", 0)

                if (branchMap.containsKey(branchName)) {
                    // 🔥 aggregate document count
                    val existing = branchMap[branchName]!!
                    branchMap[branchName] = existing.copy(
                        totalDocuments = existing.totalDocuments + docCount
                    )
                } else {
                    branchMap[branchName] = BranchDTO(
                        branchId = branchId,
                        branchName = branchName,
                        address = address,
                        csmName = csmName,
                        mobile = mobile,
                        totalDocuments = docCount
                    )
                }

                if (branchMap.size == PAGE_SIZE) break
            }

            if (records.length() < FETCH_BATCH_SIZE) {
                hasMore = false
            }

            cursor = branchMap.keys.lastOrNull()
        }

        return CursorResponse(
            content = branchMap.values.toList(),
            nextCursor = cursor,
            hasMore = hasMore
        )
    }
    fun getLais(branchId: String, search: String?, lastLai: String?): CursorResponse<LaiDTO> {

        val laiMap = LinkedHashMap<String, LaiDTO>()
        var cursor = lastLai
        var hasMore = true

        while (laiMap.size < PAGE_SIZE && hasMore) {

            val query = StringBuilder("""
            SELECT 
                CL_Contract_No_LAI__c,
                Document_Checklist__r.Opportunity__r.Name
            FROM Document_Item__c
            WHERE Document_Sent_to_Kleeto_Date__c != NULL
            AND Document_Checklist__r.Service_Disbursal__r.Branch__c = '${commonHelper.escape(branchId)}'
        """.trimIndent())

            if (!search.isNullOrBlank()) {
                query.append(" AND CL_Contract_No_LAI__c LIKE '${commonHelper.escape(search)}%' ")
            }

            if (!cursor.isNullOrBlank()) {
                query.append(" AND CL_Contract_No_LAI__c > '$cursor' ")
            }

            query.append(" ORDER BY CL_Contract_No_LAI__c ASC LIMIT $FETCH_BATCH_SIZE")

            val records = commonHelper.executeQuery(query.toString()) ?: break

            for (i in 0 until records.length()) {

                val rec = records.getJSONObject(i)

                val lai = rec.optString("CL_Contract_No_LAI__c")
                if (lai.isNullOrBlank()) continue

                val customerName = rec
                    .optJSONObject("Document_Checklist__r")
                    ?.optJSONObject("Opportunity__r")
                    ?.optString("Name")


                if (laiMap.containsKey(lai)) {
                    val existing = laiMap[lai]!!

                    laiMap[lai] = existing.copy(
                        totalFiles = existing.totalFiles + 1
                    )

                } else {
                    laiMap[lai] = LaiDTO(
                        lai = lai,
                        customerName = customerName,
                        totalFiles = 1, // ✅ first document
                    )
                }

                if (laiMap.size == PAGE_SIZE) break
            }

            if (records.length() < FETCH_BATCH_SIZE) {
                hasMore = false
            }

            cursor = laiMap.keys.lastOrNull()
        }

        return CursorResponse(
            content = laiMap.values.toList(),
            nextCursor = cursor,
            hasMore = hasMore
        )
    }

//    fun getLais(branchId: String, search: String?, lastLai: String?): CursorResponse<LaiDTO> {
//
//        val laiMap = LinkedHashMap<String, LaiDTO>()
//        var cursor = lastLai
//        var hasMore = true
//
//        while (laiMap.size < PAGE_SIZE && hasMore) {
//
//            val query = StringBuilder("""
//            SELECT
//                CL_Contract_No_LAI__c,
//                Document_Checklist__r.Opportunity__r.Name,
//                Document_Sent_to_Kleeto_Date__c
//            FROM Document_Item__c
//            WHERE Document_Sent_to_Kleeto_Date__c != NULL
//            AND Vaulting_Date__c = NULL
//            AND Document_Checklist__r.Service_Disbursal__r.Branch__c = '${commonHelper.escape(branchId)}'
//        """.trimIndent())
//
//            if (!search.isNullOrBlank()) {
//                query.append(" AND CL_Contract_No_LAI__c LIKE '${commonHelper.escape(search)}%' ")
//            }
//
//            if (!cursor.isNullOrBlank()) {
//                query.append(" AND CL_Contract_No_LAI__c > '$cursor' ")
//            }
//
//            query.append(" ORDER BY CL_Contract_No_LAI__c ASC LIMIT $FETCH_BATCH_SIZE")
//
//            val records = commonHelper.executeQuery(query.toString()) ?: break
//
//            for (i in 0 until records.length()) {
//
//                val rec = records.getJSONObject(i)
//
//                val lai = rec.optString("CL_Contract_No_LAI__c")
//                if (lai.isNullOrBlank()) continue
//
//                val customerName = rec
//                    .optJSONObject("Document_Checklist__r")
//                    ?.optJSONObject("Opportunity__r")
//                    ?.optString("Name")
//
//                val files = rec
//                    .optJSONObject("Documents_Pickup__r")
//                    ?.optInt("No_Of_Files__c", 0) ?: 0
//
//                val sentDate = rec.optString("Document_Sent_to_Kleeto_Date__c")
//
//                if (laiMap.containsKey(lai)) {
//                    val existing = laiMap[lai]!!
//                    laiMap[lai] = existing.copy(
//                        totalFiles = existing.totalFiles + files
//                    )
//                } else {
//                    laiMap[lai] = LaiDTO(
//                        lai = lai,
//                        customerName = customerName,
//                        totalFiles = files,
//                        sentToKleetoDate = sentDate
//                    )
//                }
//
//                if (laiMap.size == PAGE_SIZE) break
//            }
//
//            if (records.length() < FETCH_BATCH_SIZE) {
//                hasMore = false
//            }
//
//            cursor = laiMap.keys.lastOrNull()
//        }
//
//        return CursorResponse(
//            content = laiMap.values.toList(),
//            nextCursor = cursor,
//            hasMore = hasMore
//        )
//    }

    fun getDocuments(lai: String, lastCreatedDate: String?):  CursorResponse<DocumentDTO> {

        val query = StringBuilder("""
            SELECT Id, Name, Document_Category__c, Document_Subcategory__c, Type__c, Document_Label__c, Document_Status__c,
            CreatedDate, Document_Sent_to_Kleeto_Date__c, Document_Checklist__r.Name, Vaulting_Date__c,
            Document_Checklist__r.Number_Of_Physical_Copies__c, Document_Checklist__r.Number_Of_Scanned_Copies__c, 
            Document_Checklist__r.Number_Of_Certified_Copies__c
            FROM Document_Item__c
            WHERE CL_Contract_No_LAI__c = '$lai'
            AND Document_Sent_to_Kleeto_Date__c != NULL
        """.trimIndent())

        if (!lastCreatedDate.isNullOrBlank()) {
            query.append(" AND CreatedDate < $lastCreatedDate ")
        }

        query.append(" ORDER BY CreatedDate DESC LIMIT $PAGE_SIZE")

        val records = commonHelper.executeQuery(query.toString()) ?: JSONArray()

        val content = mutableListOf<DocumentDTO>()

        for (i in 0 until records.length()) {

            val obj = records.getJSONObject(i)

            val dto = DocumentDTO(
                id = obj.optString("Id"),
                name = obj.optString("Name"),
                category = obj.optString("Document_Category__c"),
                subCategory = obj.optString("Document_Subcategory__c"),
                label = obj.optString("Document_Label__c"),
                type = obj.optString("Type__c"),
                status = obj.optString("Document_Status__c"),
                createdDate = obj.optString("CreatedDate"),
                sentToKleeto = obj.optString("Document_Sent_to_Kleeto_Date__c"),
                physicalCopy = obj.optString("Document_Checklist__r.Number_Of_Physical_Copies__c"),
                scannedCopy = obj.optString("Document_Checklist__r.Number_Of_Scanned_Copies__c"),
                certifiedCopy = obj.optString("Document_Checklist__r.Number_Of_Certified_Copies__c"),
                lodName = obj.optJSONObject("Document_Checklist__r")?.optString("Name") ?: "",
                vaultingDate = obj.optString("Vaulting_Date__c")
            )
            println(ObjectMapper().writeValueAsString(dto))

            content.add(dto)
        }
        val nextCursor =
            if (content.isNotEmpty())
                content.last().createdDate
            else null

        return CursorResponse(
            content = content,
            nextCursor = nextCursor,
            hasMore = content.size == PAGE_SIZE
        )
    }

    fun markDocumentAsVaulted(request: MarkVaultRequest): ResponseDto {

        if (request.documentId.isBlank()) {
            return ResponseDto(false, "Document Id is required")
        }

        val vaultDate = DateTimeUtils.getCurrentDate()

        val requestBody = JSONObject().apply {
            put("Vaulting_Date__c", vaultDate)
        }

        val endpoint = "/sobjects/Document_Item__c/${request.documentId}"

        val response = sfConnection.patch(requestBody, endpoint)

        if (response == null || !response.isSuccess) {
            return ResponseDto(false, "Failed to mark document as vaulted: ${response?.message}")
        }

        return ResponseDto(
            isSuccess = true,
            message = "Document Vaulted Successfully."
        )
    }

}