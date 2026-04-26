package com.vaultlink.app.manager

import BranchDTO
import DocumentDTO
import LaiDTO
import com.vaultlink.app.dto.PickupRequest
import com.vaultlink.app.networking.EnSFObjects
import com.vaultlink.app.networking.SFConnection
import com.vaultlink.app.utills.LoggerUtils
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SalesforceManager(
    @Autowired val sfConnection: SFConnection,


    ) {
    private fun log(value: String) = LoggerUtils.log("SalesforceManager.$value")

    private fun printLog(value: String) = LoggerUtils.printLog("SalesforceManager.$value")

    private val FETCH_BATCH_SIZE = 200

    fun escape(input: String): String {
        return input.replace("'", "\\'")
    }

    fun getBranches(search: String?, lastBranch: String?, pageSize: Int): Triple<List<BranchDTO>, String?, Boolean> {
        val branchMap = LinkedHashMap<String, BranchDTO>()
        var cursor = lastBranch
        var hasMore = true

        while (branchMap.size < pageSize && hasMore) {
            val query = StringBuilder("""
                SELECT 
                    Branch_Name__r.Name,
                    Branch_Name__c,
                    Branch_Name__r.Branch_Address_line_1__c,
                    No_Of_Files__c,
                    Owner.Phone, Owner.Name
                FROM Documents_Pickup__c
                WHERE Pickup_Stage__c = 'Delivered'
            """.trimIndent())

            if (!search.isNullOrBlank()) {
                query.append(" AND Branch_Name__r.Name LIKE '${escape(search)}%' ")
            }
            if (!cursor.isNullOrBlank()) {
                query.append(" AND Branch_Name__r.Name > '$cursor' ")
            }

            query.append(" ORDER BY Branch_Name__r.Name ASC LIMIT $FETCH_BATCH_SIZE")

            val response = sfConnection.get(query.toString())
            print("Response from SF: ")
            println(response)
            val records = response?.optJSONArray("records") ?: break

            for (i in 0 until records.length()) {
                val rec = records.getJSONObject(i)
                print("One record of SF: ")
                println(rec)
                val branchObj = rec.optJSONObject("Branch_Name__r") ?: continue
                val branchName = branchObj.optString("Name")
                val branchId = rec.optString("Branch_Name__c")

                if (branchName.isBlank()) continue

                val address = branchObj.optString("Branch_Address_line_1__c")
                val owner = rec.optJSONObject("Owner")
                val csmName = owner?.optString("Name")
                val mobile = owner?.optString("Phone") ?: owner?.optString("MobilePhone")

                val docCount = rec.optInt("No_Of_Files__c", 0)

                if (branchMap.containsKey(branchName)) {
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

                if (branchMap.size == pageSize) break
            }

            if (records.length() < FETCH_BATCH_SIZE) {
                hasMore = false
            }
            cursor = branchMap.keys.lastOrNull() ?: cursor
        }
        print("Response of getBranches: ")
        println(branchMap.values.toList())

        return Triple(branchMap.values.toList(), cursor, hasMore)
    }

    fun getLais(branchId: String, search: String?, lastLai: String?, pageSize: Int): Triple<List<LaiDTO>, String?, Boolean> {
        val laiMap = LinkedHashMap<String, LaiDTO>()
        var cursor = lastLai
        var hasMore = true

        while (laiMap.size < pageSize && hasMore) {
            val query = StringBuilder("""
                SELECT 
                    CL_Contract_No_LAI__c,
                    Document_Checklist__r.Opportunity__r.Name
                FROM Document_Item__c
                WHERE Document_Sent_to_Kleeto_Date__c != NULL
                AND Document_Checklist__r.Service_Disbursal__r.Branch__c = '${escape(branchId)}'
            """.trimIndent())

            if (!search.isNullOrBlank()) {
                query.append(" AND CL_Contract_No_LAI__c LIKE '${escape(search)}%' ")
            }
            if (!cursor.isNullOrBlank()) {
                query.append(" AND CL_Contract_No_LAI__c > '$cursor' ")
            }

            query.append(" ORDER BY CL_Contract_No_LAI__c ASC LIMIT $FETCH_BATCH_SIZE")

            val response = sfConnection.get(query.toString())
            val records = response?.optJSONArray("records") ?: break

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
                        totalFiles = 1
                    )
                }

                if (laiMap.size == pageSize) break
            }

            if (records.length() < FETCH_BATCH_SIZE) {
                hasMore = false
            }
            cursor = laiMap.keys.lastOrNull() ?: cursor
        }

        return Triple(laiMap.values.toList(), cursor, hasMore)
    }

    fun getDocuments(lai: String, lastCreatedDate: String?, pageSize: Int): List<DocumentDTO> {
        val query = StringBuilder("""
            SELECT Id, Name, Document_Category__c, Document_Subcategory__c, Type__c, Document_Label__c, Document_Status__c,
            CreatedDate, Document_Sent_to_Kleeto_Date__c, Document_Checklist__r.Name, Vaulting_Date__c,
            Document_Checklist__r.Number_Of_Physical_Copies__c, Document_Checklist__r.Number_Of_Scanned_Copies__c, 
            Document_Checklist__r.Number_Of_Certified_Copies__c
            FROM Document_Item__c
            WHERE CL_Contract_No_LAI__c = '${escape(lai)}'
            AND Document_Sent_to_Kleeto_Date__c != NULL
        """.trimIndent())

        if (!lastCreatedDate.isNullOrBlank()) {
            query.append(" AND CreatedDate < $lastCreatedDate ")
        }

        query.append(" ORDER BY CreatedDate DESC LIMIT $pageSize")

        val response = sfConnection.get(query.toString())
        val records = response?.optJSONArray("records") ?: JSONArray()

        val content = mutableListOf<DocumentDTO>()
        for (i in 0 until records.length()) {
            val obj = records.getJSONObject(i)
            val checklistObj = obj.optJSONObject("Document_Checklist__r")
            
            content.add(DocumentDTO(
                id = obj.optString("Id"),
                name = obj.optString("Name"),
                category = obj.optString("Document_Category__c"),
                subCategory = obj.optString("Document_Subcategory__c"),
                label = obj.optString("Document_Label__c"),
                type = obj.optString("Type__c"),
                status = obj.optString("Document_Status__c"),
                createdDate = obj.optString("CreatedDate"),
                sentToKleeto = obj.optString("Document_Sent_to_Kleeto_Date__c"),
                physicalCopy = checklistObj?.optString("Number_Of_Physical_Copies__c") ?: "",
                scannedCopy = checklistObj?.optString("Number_Of_Scanned_Copies__c") ?: "",
                certifiedCopy = checklistObj?.optString("Number_Of_Certified_Copies__c") ?: "",
                lodName = checklistObj?.optString("Name") ?: "",
                vaultingDate = obj.optString("Vaulting_Date__c")
            ))
        }
        return content
    }

    fun updateDocumentsVaultingDate(
        documentIds: List<String>,
        vaultingDate: String
    ): Boolean {

        if (documentIds.isEmpty()) {
            return false
        }

        for (documentId in documentIds) {
            val requestBody = JSONObject()
                .put("Vaulting_Date__c", vaultingDate)

            val endpoint = "/sobjects/Document_Item__c/$documentId"

            val response = sfConnection.patch(requestBody, endpoint)

            if (response?.isSuccess != true) {
                return false
            }
        }

        return true
    }


    fun fetchPickupRequestsByStatus(status: String?): List<PickupRequest>? {
        if (status.isNullOrBlank()) return null

        val query =
            """
                SELECT 
                Id,
                OwnerId,
 TYPEOF Owner
        WHEN User THEN MobilePhone, Name
    END,
Branch_Name__r.Branch_Address_line_1__c, 
Branch_Name__r.Branch_Address_line_2__c, 
                IsDeleted,
                Name,
                CreatedDate,
                CreatedById,
                LastModifiedDate,
                LastModifiedById,
                LastActivityDate,
                LastViewedDate,
                LastReferencedDate,
                Actual_Pickup_Date__c,
                BM_BMD__c,
                Branch_Name__r.Name,
                Consignment_ID__c,
                Shipment_Recieved_Date__c,
                Expected_Pickup_Date__c,
                No_Of_Files__c,
                Number_Of_Boxes__c,
                Pickup_Stage__c,
                Requested_Pickup_Date__c
                FROM Documents_Pickup__c where Pickup_Stage__c = '$status'
                """.trimIndent()

        val response = sfConnection.get(query) ?: return null
        val records = response.optJSONArray("records") ?: return emptyList()

        val result = mutableListOf<PickupRequest>()
        for (i in 0 until records.length()) {
            val r = records.getJSONObject(i)
            val branchObj = r.optJSONObject("Branch_Name__r")
            val ownerObj = r.optJSONObject("Owner")

            result.add(
                PickupRequest(
                    id = r.optString("Id", r.optString("Name", "")),
                    branchName = branchObj?.optString("Name") ?: "—",
                    branchAddress = branchObj?.optString("Branch_Address_line_1__c") ?: "—",
                    csmBM = r.optString("BM_BMD__c", "—"),
                    mobile = "—", // Field not in query yet
                    noOfFiles = r.optInt("No_Of_Files__c", 0),
                    noOfBoxes = r.optInt("Number_Of_Boxes__c", 0),
                    requestedDate = r.optString("Requested_Pickup_Date__c", r.optString("Pickup_Date__c", "—")),
                    consignmentId = r.optString("Consignment_ID__c", "—"),
                    ownerName = ownerObj?.optString("Name") ?: "—",
                    status = r.optString("Pickup_Stage__c", "—"),
                    actualPickupDate = r.optString("Actual_Pickup_Date__c", "—")
                )
            )
        }
        return result
    }

    fun updatePickupRequest(
        recordId: String?,
        estimatedDate: String?,
        pod: String?,
        status: String? = null,
        actualDate: String? = null,
        deliveryDate: String? = null
    ): Boolean {

        if (recordId.isNullOrBlank()) return false

        val requestObject = JSONObject().apply {
            if (!status.isNullOrBlank()) {
                put("Pickup_Stage__c", status)
            } else {
                if (!estimatedDate.isNullOrBlank()) {
                    put("Pickup_Stage__c", "Scheduled")
                }
            }

            if (!estimatedDate.isNullOrBlank()) {
                put("Expected_Pickup_Date__c", estimatedDate)
            }

            if (!actualDate.isNullOrBlank()) {
                put("Actual_Pickup_Date__c", actualDate)
                put("Pickup_Stage__c", "Intransit")
            }

            if (!deliveryDate.isNullOrBlank()) {
                put("Shipment_Recieved_Date__c", deliveryDate)
                put("Pickup_Stage__c", "Delivered")
            }

            if (!pod.isNullOrBlank()) {
                put("Consignment_ID__c", pod)
            }
        }

        if (requestObject.length() == 0) return true

        // Build the PATCH endpoint using the Salesforce record Id
        val endpoint = "/sobjects/Documents_Pickup__c/$recordId"

        println("Updating Salesforce record: $endpoint with data: $requestObject")

        val sfResponse = sfConnection.patch(requestObject, endpoint)

        if (sfResponse == null) {
            log("updatePickupRequest - Null response from Salesforce PATCH for recordId: $recordId")
            return false
        }

        return if (sfResponse.isSuccess) {
            log("updatePickupRequest - Updated Salesforce record successfully. Id: $recordId")
            true
        } else {
            log("updatePickupRequest - Failed to update record. Id: $recordId, Response: $sfResponse")
            false
        }
    }
}
