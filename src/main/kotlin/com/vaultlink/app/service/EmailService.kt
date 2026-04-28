package com.vaultlink.app.service

import com.vaultlink.app.dto.MFile
import com.vaultlink.app.dto.PickupRequest
import com.vaultlink.app.helper.MailHelper
import com.vaultlink.app.model.VaultLaiAcknowledgement
import com.vaultlink.app.repository.VaultLaiAckRepository
import com.vaultlink.app.security.AppProperty
import com.vaultlink.app.utills.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service("mailer")
class EmailService(
    @Autowired private val appProperty: AppProperty,
    @Autowired private val mailHelper: MailHelper,
    @Autowired val vaultLaiAckRepository: VaultLaiAckRepository
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
        val subject = "Pickup Scheduled - ${pickup.branchName}"

        val sfRecordUrl = "${appProperty.sfUIURL}/lightning/r/Documents_Pickup__c/${pickup.id}/view"

        val bodyText = """
            
            Below are the details for the scheduled pickup:

            Salesforce Record : $sfRecordUrl
            Branch Name       : ${pickup.branchName}
            Branch Address    : ${pickup.branchAddress}
            CSM / BM          : ${pickup.csmBM}
            Requested Date    : ${pickup.requestedDate}
            Scheduled Date    : ${pickup.expectedPickupDate ?: "—"}
            Current Stage     : Pickup Scheduled

            Please take the necessary actions to ensure documents are ready.

            Regards,
            Tech Team
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

    @Async
    fun sendAcknowledgementMailAsync(
        records: List<VaultLaiAcknowledgement>,
        acknowledgedAt: String
    ) {
        try {

            val acknowledgedDateTime =  DateTimeUtils.getStringFromDateTimeString(
                acknowledgedAt,DateTimeFormat.yyyy_MM_dd, DateTimeFormat.dd_MM_yyyy)

            val totalAcknowledged = records.size

            val sb = StringBuilder()
            sb.append("Please find the LAI acknowledgement details below:")
            sb.append("\n\nTotal LAIs Acknowledged : $totalAcknowledged")
            sb.append("\nAcknowledged At : $acknowledgedDateTime")
            sb.append("\n\nAcknowledged LAIs:")

            records.forEachIndexed { index, record ->
                sb.append("\n${index + 1}. ${record.lai}")
            }

            sb.append("\n\n\nThis is an auto generated email. Please do not reply.")
            sb.append("\n- Homefirst")

            val mailSent = mailHelper.sendMimeMessage(
                arrayOf("Kainaat.zaidi@homefirstindia.com"),
                "Vault Management - LAI Acknowledgement",
                sb.toString(),
                cc = arrayOf(
                    KAINAAT_EMAIL_ID
                    // RANAN_EMAIL_ID, SANJAY_EMAIL_ID, TANYA_EMAIL_ID
                )
            )

            if (mailSent) {
                records.forEach {
                    it.isNotified = true
                }

                vaultLaiAckRepository.saveAll(records)
            }

        } catch (e: Exception) {
            LoggerUtils.log("Failed to send acknowledgement mail: ${e.message}")
        }
    }

}

