//package com.vaultlink.app.service
//
//import com.vaultlink.app.dto.PickupRequest
//import com.vaultlink.app.dto.UtilsMFile
//import com.vaultlink.app.security.AppProperty
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.stereotype.Service
//
//
//@Service("mailer")
//class EmailService(
//    @Autowired private val appProperty: AppProperty
//) {
//    private val logger = LoggerFactory.getLogger(EmailService::class.java)
//
//    /**
//     * Standardized sendMail method matching the user's preferred pattern.
//     */
//    fun sendMail(
//        toEmails: List<String>,
//        subject: String,
//        bodyText: String,
//        ccEmails: List<String> = emptyList(),
//        attachmentFiles: List<UtilsMFile> = emptyList(),
//        isHtml: Boolean = true
//    ) {
//
//        if (!appProperty.runScheduler) return
//
//        if (toEmails.isEmpty()) {
//            logger.warn("Cannot send email: recipient list is empty")
//            return
//        }
//
//        try {
//            mailer.sendMail(
//                toEmails = listOf(SANJAY_EMAIL_ID),
//                subject = "Monthly Digital KYC Report (${prevMonth.month} ${prevMonth.year})",
//                bodyText = reportText,
//                ccEmails = arrayListOf(RUPESH_EMAIL_ID, TANYA_EMAIL_ID),
//                attachmentFiles = attachments,
//                isHtml = true
//            )
//        } finally {
//            attachments.forEach { it.file.delete() }
//        }
//    }
//
//    fun sendPickupScheduledEmail(toEmail: String, pickup: PickupRequest) {
//        val subject = "Pickup Scheduled Notification - ${pickup.branchName}"
//
//        // Formatting with Dashes as requested in the "Report Style"
//        val bodyText = """
//            <div style="font-family: 'Courier New', Courier, monospace; color: #333; max-width: 600px; padding: 20px; border: 1px solid #ccc;">
//                <h3 style="color: #2c3e50; border-bottom: 1px solid #2c3e50;">Notification: Pickup Scheduled</h3>
//                <p>Below are the details for the scheduled pickup:</p>
//
//                <p>
//                <b>Record ID</b> ------------------ ${pickup.id}<br/>
//                <b>Branch Name</b> ---------------- ${pickup.branchName}<br/>
//                <b>Branch Address</b> ------------- ${pickup.branchAddress}<br/>
//                <b>CSM / BM</b> ------------------- ${pickup.csmBM}<br/>
//                <b>Requested Date</b> ------------- ${pickup.requestedDate}<br/>
//                <b>Scheduled Date</b> ------------- <span style="color: #e67e22; font-weight: bold;">${if (pickup.status == "Scheduled") "Scheduled" else pickup.requestedDate}</span><br/>
//                <b>Pickup Stage</b> --------------- <span style="color: #27ae60; font-weight: bold;">${pickup.status}</span><br/>
//                </p>
//
//                <p>Please take the necessary actions.</p>
//                <br/>
//                <p>Regards,<br/><b>VaultLink System</b></p>
//            </div>
//        """.trimIndent()
//
//        sendMail(
//            toEmails = listOf(toEmail),
//            subject = subject,
//            bodyText = bodyText,
//            isHtml = true
//        )
//    }
//}
