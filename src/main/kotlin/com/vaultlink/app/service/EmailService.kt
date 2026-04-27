package com.vaultlink.app.service

import com.vaultlink.app.dto.MFile
import com.vaultlink.app.dto.PickupRequest
import com.vaultlink.app.helper.MailHelper
import com.vaultlink.app.security.AppProperty
import com.vaultlink.app.utills.KAINAAT_EMAIL_ID
import com.vaultlink.app.utills.RANAN_EMAIL_ID
import com.vaultlink.app.utills.SANJAY_EMAIL_ID
import com.vaultlink.app.utills.TANYA_EMAIL_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("mailer")
class EmailService(
    @Autowired private val appProperty: AppProperty,
    @Autowired private val mailHelper: MailHelper
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    /**
     * Standardized sendMail method using MailHelper.
     */
    fun sendMail(
        toEmails: List<String>,
        subject: String,
        bodyText: String,
        ccEmails: List<String> = emptyList(),
        attachmentFiles: List<MFile> = emptyList(),
        isHtml: Boolean = true
    ) {

        if (toEmails.isEmpty()) {
            logger.warn("Cannot send email: recipient list is empty")
            return
        }

        val mFiles = if (attachmentFiles.isNotEmpty()) {
            ArrayList(attachmentFiles.map { MFile(it.name, it.path) })
        } else null

        val success = mailHelper.sendMimeMessage(
            to = toEmails.toTypedArray(),
            subject = subject,
            body = bodyText,
            isHtml = isHtml,
            files = mFiles,
            cc = if (ccEmails.isNotEmpty()) ccEmails.toTypedArray() else null
        )

        if (success) {
            logger.info("Successfully sent email: '$subject' to $toEmails")
        } else {
            logger.error("Failed to send email: '$subject' to $toEmails")
        }
    }

    fun sendPickupScheduledEmail(toEmail: String, pickup: PickupRequest) {
        val subject = "Action Required: Pickup Scheduled - ${pickup.branchName}"

        val bodyText = """
            Notification: Pickup Scheduled
            --------------------------------------------------
            Below are the details for the scheduled pickup:

            Record ID ------------------ ${pickup.id}
            Branch Name ---------------- ${pickup.branchName}
            Branch Address ------------- ${pickup.branchAddress}
            CSM / BM ------------------- ${pickup.csmBM}
            Requested Date ------------- ${pickup.requestedDate}
            Scheduled Date ------------- ${pickup.expectedPickupDate ?: "—"}
            Current Stage -------------- Pickup Scheduled

            Please take the necessary actions to ensure documents are ready.

            Regards,
            VaultLink System
            --------------------------------------------------
            This is an auto generated email. Please do not reply.
        """.trimIndent()

        sendMail(
            toEmails = listOf(toEmail),
            subject = subject,
            bodyText = bodyText,
            ccEmails = listOf(KAINAAT_EMAIL_ID, TANYA_EMAIL_ID, TANYA_EMAIL_ID, TANYA_EMAIL_ID),
            isHtml = false
        )
    }

}

