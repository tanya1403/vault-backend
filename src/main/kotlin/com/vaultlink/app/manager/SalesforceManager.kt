package com.vaultlink.app.manager

import com.vaultlink.app.networking.EnSFObjects
import com.vaultlink.app.networking.SFConnection
import com.vaultlink.app.security.AppProperty
import com.vaultlink.app.utills.LoggerUtils
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SalesforceManager(
    @Autowired val sfConnection: SFConnection,
    @Autowired val appProperty: AppProperty,


    ) {
    private fun log(value: String) = LoggerUtils.log("SalesforceManager.$value")

    private fun printLog(value: String) = LoggerUtils.printLog("SalesforceManager.$value")


    fun fetchPickupRequestsByStatus(status: String?): org.json.JSONArray? {
        if (status.isNullOrBlank()) return null

        val query =
            """
           SELECT Id,
           RecordTypeId,
           X_Sell_Products_Services_Dispersals__r.Id,
           X_Sell_Products_Services_Dispersals__r.Top_Up_or_LTV_EnhancementAmount_Required__c,
           X_Sell_Products_Services_Dispersals__r.Processing_Fee_for_LTV_Top_Up__c,
           X_Sell_Products_Services_Dispersals__r.Top_Up_or_Loan_Enhancemet_EMI__c,
           X_Sell_Products_Services_Dispersals__r.Tenure_Yrs__c,
           X_Sell_Products_Services_Dispersals__r.Top_Up_Number_of_EMI__c,
           X_Sell_Products_Services_Dispersals__r.CL_Contract_No_LAI__c,
           Opportunity__r.Primary_Contact_Name__c,
           Opportunity__r.Co_Applicant_Name1__r.Name,
           Opportunity__r.Property_Code__c,
           Opportunity__r.Property_Name__c,
           Opportunity__r.Property_Area_Sq_Ft__c,
           Opportunity__r.Property__c,
           Opportunity__r.Property_Phase__c,
           Opportunity__r.Block_Building_No__c,
           Opportunity__r.Block_Bldg_Name__c,
           Opportunity__r.Unit_No__c,
           Opportunity__r.Property_Type__c,
           Opportunity__r.Property_Unit_Type__c,
           Opportunity__r.Basic_Rate_Per_Sq_Ft__c,
           Opportunity__r.Rural_or_Urban__c,
           Opportunity__r.Interest_Type__c,
           Opportunity__r.Offset_to_HFFC_PLR__c,
           Opportunity__r.Loan_Purpose__c
           FROM Service_and_Disbursal__c
           WHERE X_Sell_Products_Services_Dispersals__r.RecordTypeId = '01290000001AWRTAA4' and Status__c = '$status'
           """.trimIndent()
        val response = sfConnection.get(query) ?: return null

        return response.optJSONArray("records")
    }

//    fun updatePickupRequest(consignmentId: String, estimatedDate: String): JSONArray? {
//        if (consignmentId.isBlank() || estimatedDate.isBlank()) return null
//
//        val requestObject = JSONObject().apply {
//            put("Estimated_Pickup_Date__c", estimatedDate)
//        }
//        val endpoint = "${EnSFObjects.SERVICE_AND_DISBURSAL.value}$consignmentId"
//        println("Updating Salesforce record: $endpoint with data: $requestObject")
//         val response = sfConnection.patch(requestObject, endpoint)
//
//        return response
//
//    }
}
