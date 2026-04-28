package com.vaultlink.app.security

import com.vaultlink.app.utills.decryptAnyKey
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.annotation.Validated
import java.util.Properties

@Validated
@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    @field:Size(min = 32, message = "app.jwt.secret must be at least 32 characters")
    var secret: String = "",
    @field:Min(value = 60_000, message = "app.jwt.expiration-ms must be at least 60000")
    var expirationMs: Long = 900_000,
    @field:Min(value = 300_000, message = "app.jwt.refresh-expiration-ms must be at least 300000")
    var refreshExpirationMs: Long = 604_800_000,
)

@Validated
@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    var allowedOrigins: List<String> = emptyList(),
)

@Validated
@ConfigurationProperties(prefix = "app.seed")
data class SeedProperties(
    var enabled: Boolean = false,
    var username: String = "",
    var password: String = "",
    var fullName: String = "VaultLink Admin",
)

enum class EnvProfile(
    val value : String
) {

    DEV("dev"),
    STAGING("staging"),
    UAT("uat"),
    PROD("prod")

}

@Configuration
@EnableConfigurationProperties(JwtProperties::class, CorsProperties::class, SeedProperties::class)
class AppProperty {

//    companion object {
//        private var _gDnrCred: Creds? = null
//    }
//
//    private fun gDnrCred(): Creds? {
//        if (null == _gDnrCred) {
//            _gDnrCred = credentialManager.fetchCredentials(
//                EnPartnerName.GOOGLE_DNR,
//                EnCredType.PRODUCTION
//            )
//
//            _gDnrCred?.apply {
//                username = decryptAnyKey(username!!)
//                password = decryptAnyKey(password!!)
//            }
//
//        }
//        return _gDnrCred
//    }

    @Bean
    fun getJavaMailSender(): JavaMailSender {

        val mailSender = JavaMailSenderImpl()
        mailSender.host = "smtp.gmail.com"
        mailSender.port = 587
        mailSender.username = decryptAnyKey("ANVdPz/E1dCv/rtuPhl3liYmqgSm9VCSd82uCBwbmGA=")
        mailSender.password = decryptAnyKey("A4x22x/S3bNyvDJQzLP738qurp6MpBGs8SfEFg9xAao=")

        val props: Properties = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = true
        props["mail.smtp.starttls.enable"] = true
        props["mail.debug"] = true

        return mailSender

    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Value("\${spring.profiles.active:local}")
    lateinit var activeProfile : String

    @Value("\${application.flags.isStrictProduction:false}")
    var isStrictProduction : Boolean = false

    fun isProduction() = activeProfile == EnvProfile.PROD.value

    fun isUAT() = activeProfile == EnvProfile.UAT.value

    fun isStaging() = activeProfile == EnvProfile.DEV.value

    @Value("\${application.key.salt:}")
    lateinit var salt: String

    @Value("\${application.key.mamasSpaghetti:}")
    lateinit var  mamasSpaghetti: String

    @Value("\${application.key.mamasSalt:}")
    lateinit var  mamasSalt: String

    @Value("\${spring.mail.username:}")
    lateinit var senderEmail: String

    @Value("\${application.salesforce.username:}")
    lateinit var sfUserName: String

    @Value("\${application.salesforce.url:}")
    lateinit var sfURL: String

    @Value("\${application.salesforce.UIurl:}")
    lateinit var sfUIURL: String

    @Value("\${application.salesforce.clientID:}")
    lateinit var sfClientID: String

}
