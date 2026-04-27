package com.vaultlink.app.helper

import com.vaultlink.app.dto.MFile
import com.vaultlink.app.security.AppProperty
import com.vaultlink.app.utills.LoggerUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.io.File
import jakarta.mail.SendFailedException
import jakarta.mail.internet.MimeMessage

@Component
class MailHelper(
    @Autowired val mailSender: JavaMailSender,
    @Autowired val appProperty: AppProperty
) {

    private fun log(value: String) = LoggerUtils.log("MailHelper.$value")

    fun sendSimpleMessage(
        to: Array<String>,
        subject: String,
        body: String,
        cc: Array<String>? = null
    ) : Boolean {

        return try {

            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true)
            helper.setFrom(appProperty.senderEmail)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(body, false)

            if (cc?.isNotEmpty() == true)
                helper.setCc(cc)

            mailSender.send(message)

            true

        } catch (e: SendFailedException) {
            log("sendSimpleMessage - Error : ${e.message}")
            false
        }

    }

    fun sendMimeMessage(
        to: Array<String>,
        subject: String,
        body: String,
        isHtml: Boolean = false,
        files: ArrayList<MFile>? = null,
        cc: Array<String>? = null
    ) : Boolean {

        return try {

            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true)
            helper.setFrom(appProperty.senderEmail)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(body, isHtml)

            if (cc?.isNotEmpty() == true)
                helper.setCc(cc)

            files?.forEach {
                val file = FileSystemResource(File(it.path))
                helper.addAttachment(it.name, file)
            }

            mailSender.send(message)
            true

        } catch (e: SendFailedException) {
            log("sendMimeMessage - Error : ${e.message}")
            false
        }

    }

}