package com.vaultlink.app.config

import com.vaultlink.app.manager.SalesforceManager
import com.vaultlink.app.service.EmailService
import com.vaultlink.app.security.AppProperty
import com.vaultlink.app.utills.Constants
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class PickupReportScheduler(
    private val sfManager: SalesforceManager,
    private val emailService: EmailService,
    private val appProperty: AppProperty
) {
    private val logger = LoggerFactory.getLogger(PickupReportScheduler::class.java)

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    fun sendDailyPickupReport() {
        if (!appProperty.runScheduler) {
            logger.info("Scheduler is disabled by configuration (runScheduler=false)")
            return
        }

        logger.info("Scheduler triggered for Daily Pickup Report: ${LocalDateTime.now()}")

        val reportDate = LocalDate.now().minusDays(1)
        
        try {
            // Fetch records by status to build summary
            val newRequests = sfManager.fetchPickupRequestsByStatus("New") ?: emptyList()
            val scheduledRequests = sfManager.fetchPickupRequestsByStatus("Scheduled") ?: emptyList()
            val inTransitRequests = sfManager.fetchPickupRequestsByStatus("Intransit") ?: emptyList()
            val deliveredRequests = sfManager.fetchPickupRequestsByStatus("Delivered") ?: emptyList()

            val reportBody = buildPickupSummaryReport(
                newCount = newRequests.size,
                scheduledCount = scheduledRequests.size,
                inTransitCount = inTransitRequests.size,
                deliveredCount = deliveredRequests.size,
                reportDate = reportDate.toString()
            )

            emailService.sendMail(
                toEmails = listOf(Constants.SANJAY_EMAIL_ID, Constants.TANYA_EMAIL_ID),
                subject = "Daily Pickup Summary Report - $reportDate",
                bodyText = reportBody,
                ccEmails = listOf(Constants.SUDHISH_EMAIL_ID),
                isHtml = true
            )
            
            logger.info("Daily Pickup Report sent successfully for $reportDate")
        } catch (e: Exception) {
            logger.error("Failed to generate/send daily pickup report", e)
        }
    }

    private fun buildPickupSummaryReport(
        newCount: Int,
        scheduledCount: Int,
        inTransitCount: Int,
        deliveredCount: Int,
        reportDate: String
    ): String {
        val total = newCount + scheduledCount + inTransitCount + deliveredCount
        
        return """
            <div style="font-family: 'Courier New', Courier, monospace; color: #333; max-width: 600px; padding: 20px; border: 1px solid #ccc;">
                <h3 style="color: #2c3e50; border-bottom: 1px solid #2c3e50;">Daily Pickup Summary Report</h3>
                <p>Below are the details of Pickup requests for <b>Period: $reportDate</b></p>
                
                <p>
                <b>New Requests</b> ------------------------- $newCount<br/>
                <b>Scheduled</b> ---------------------------- $scheduledCount<br/>
                <b>In-Transit</b> ---------------------------- $inTransitCount<br/>
                <b>Delivered</b> ----------------------------- $deliveredCount<br/>
                <br/>
                <b>Total Records Affected</b> ---------------- $total<br/>
                </p>
                
                <p>
                <b>Note:</b><br/>
                This report provides a snapshot of current pickup stages in Salesforce.
                </p>
                <br/>
                <p>Regards,<br/><b>VaultLink System</b></p>
                <hr style="border: none; border-top: 1px solid #eee; margin-top: 20px;">
                <p style="font-size: 11px; color: #7f8c8d; text-align: center;">This is an automated system generated report.</p>
            </div>
        """.trimIndent()
    }
}
