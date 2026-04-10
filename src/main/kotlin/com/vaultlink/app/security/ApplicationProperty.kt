package com.vaultlink.app.security

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.annotation.Validated

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

    @Value("\${application.flags.isSalesforceLive:false}")
    var isSalesforceLive: Boolean = false

    @Value("\${application.key.mamasSpaghetti:}")
    lateinit var  mamasSpaghetti: String

    @Value("\${application.path.fileIdentifierURL:}")
    lateinit var  fileIdentifierURL: String

    @Value("\${application.key.mamasSalt:}")
    lateinit var  mamasSalt: String

    @Value("\${application.path.files:}")
    lateinit var  filePath: String

    @Value("\${application.path.smsDispositionURL:}")
    lateinit var smsDispositionURL: String

    @Value("\${application.s3Bucket.name:}")
    lateinit var s3BucketName: String

    @Value("\${application.s3LogBucket.name:}")
    lateinit var s3LogBucketName: String

    @Value("\${application.s3Bucket.region:}")
    lateinit var s3BucketRegion: String

    @Value("\${spring.mail.username:}")
    lateinit var senderEmail: String

    @Value("\${application.path.callDispositionURL:}")
    lateinit var callDispositionURL: String

    @Value("\${application.path.pulseCallDispositionURL:}")
    lateinit var pulseCallDispositionURL: String

    @Value("\${application.path.digiDispositionURL:}")
    lateinit var digiDispositionURL: String

    @Value("\${application.key.runScheduler:false}")
    var  runScheduler: Boolean = false

    @Value("\${application.key.backupLog:false}")
    var  backupLog: Boolean = false

    @Value("\${application.key.isStrictProdProcessActive:false}")
    var  isStrictProdProcessActive: Boolean = false

    @Value("\${application.key.msg.templateId:}")
    lateinit var  msgTemplateId: String

    @Value("\${application.key.google_group_orcas:}")
    lateinit var  googleGroupOrcasCallbackUrl: String

    @Value("\${application.salesforce.username:}")
    lateinit var sfUserName: String

    @Value("\${application.salesforce.url:}")
    lateinit var sfURL: String

    @Value("\${application.salesforce.clientID:}")
    lateinit var sfClientID: String

    @Value("\${application.aws.accessKey:}")
    lateinit var awsAccessKey: String

    @Value("\${application.aws.secretKey:}")
    lateinit var awsSecretKey: String

    @Value("\${application.hffc.certificate:}")
    lateinit var  hffcCertificate: String

    @Value("\${application.hffc.privatekey:}")
    lateinit var  hffcPrivateKey: String

    @Value("\${application.axis.encryption:}")
    lateinit var axisEncryptionKey: String

    @Value("\${application.axis.p12:}")
    lateinit var axisP12Key: String
}
