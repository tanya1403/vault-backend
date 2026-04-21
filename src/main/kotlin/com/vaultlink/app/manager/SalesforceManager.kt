package com.vaultlink.app.manager

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
