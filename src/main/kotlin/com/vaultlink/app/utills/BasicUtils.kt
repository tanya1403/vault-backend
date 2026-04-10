package com.vaultlink.app.utills

import com.vaultlink.app.utills.LoggerUtils.log
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.io.FileUtils
import org.springframework.core.io.ClassPathResource
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

data class BasicAuthCreds(
    val clientId: String,
    val clientSecret: String,
)

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun addRolePrefix(role: String) = "ROLE_$role"

fun getRandomUUID(): String = UUID.randomUUID().toString()

fun isNotNullOrNA(value: String?): Boolean {
    return (null != value && !value.equals(NA, ignoreCase = true) && !value.equals("Null", ignoreCase = true)
            && value.isNotEmpty())
}

enum class MimeMap(val mime: String, val extention: String) {
    PDF("application/pdf", ".pdf"),
    AUDIO("audio/mpeg", ".mp3");

    companion object {
        fun mapMimetoExt(mime: String): String {
            for (item in values()) {
                if (item.mime == mime) return item.extention
            }
            return "Unknown"
        }

        fun mapExtToMime(ext: String): String {
            for (item in values()) {
                if (item.extention == ext) return item.mime
            }
            return "Unknown"
        }
    }
}

@Throws(UnsupportedEncodingException::class)
fun getClientCreds(authorizationHeader: String): BasicAuthCreds {

    val decodedBytes = Base64.getDecoder().decode(authorizationHeader.replaceFirst("Basic ".toRegex(), ""))
    val clientCredsString = String(decodedBytes, Charsets.UTF_8)
    val tokenizer = StringTokenizer(clientCredsString, ":")
    val clientId = tokenizer.nextToken()
    val clientSecret = tokenizer.nextToken()

    return BasicAuthCreds(clientId, clientSecret)

}

fun downloadFileFromUrl(downloadUrl: String, filePath: String): Boolean {

    val fileOS: FileOutputStream?

    return try {

        val inputStream = BufferedInputStream(URL(downloadUrl).openStream())
        fileOS = FileOutputStream(filePath)
        val data = ByteArray(1024)
        var byteContent: Int
        while (inputStream.read(data, 0, 1024).also { byteContent = it } != -1) {
            fileOS.write(data, 0, byteContent)
        }

        fileOS.flush()
        fileOS.close()
        true

    } catch (ioe: Exception) {
        log("Error while saving file: $ioe")
        ioe.printStackTrace()
        false
    }
}

fun downloadFileFromUrl(url: URL, filePath: String): Boolean {

    return try {
        url.openStream().use { inp ->
            BufferedInputStream(inp).use { bis ->
                FileOutputStream(filePath).use { fos ->
                    val data = ByteArray(1024)
                    var count: Int
                    while (bis.read(data, 0, 1024).also { count = it } != -1) {
                        fos.write(data, 0, count)
                    }
                }
            }
        }
        true
    } catch (ioe: Exception) {
        log("Error while saving file: $ioe")
        ioe.printStackTrace()
        false
    }
}

fun generateFileName(identifier: String?, expectedFileName: String): String? {

    val fileName = StringBuilder()

    if (identifier.isNotNullOrNA()) {
        fileName.append(identifier)
        fileName.append("_")
    }

    fileName.append(System.currentTimeMillis())
    fileName.append("_")

    if (!expectedFileName.contains("."))
        return null

    fileName.append(expectedFileName)
    return fileName.toString()

}


fun isLTVInRange(
    loanAmount: Double,
    propertyValue: Double
): Boolean {

    return when (loanAmount) {
        in 1.0..3000000.0 -> (loanAmount / propertyValue) * 100.0 <= 90
        in 3000000.0..7000000.0 -> (loanAmount / propertyValue) * 100.0 <= 80
        else -> (loanAmount / propertyValue) * 100.0 <= 75
    }

}

fun getIPAddress(request: HttpServletRequest): String {

    var ipAddress = request.getHeader("X-FORWARDED-FOR")
    if (null == ipAddress) ipAddress = request.remoteAddr
    return ipAddress

}

@Throws(IOException::class)
fun getBase64FromFile(filePath: String?): String {
    val fileContent: ByteArray = FileUtils.readFileToByteArray(File(filePath!!))
    return Base64.getEncoder().encodeToString(fileContent)
}

fun isLoanTopUp(loanType: String): Boolean {
    val type = loanType.lowercase(Locale.getDefault())
    return type.contains("top-up") || type.contains("topup") || type.contains("top up")
}

fun getGender(gender: String): String {
    return if (gender.equals("male", ignoreCase = true) || gender.equals(
            "m",
            ignoreCase = true
        )
    ) "Male" else if (gender.equals("female", ignoreCase = true) || gender.equals(
            "f",
            ignoreCase = true
        )
    ) "Female" else if (gender.equals("Others", ignoreCase = true) || gender.equals(
            "Other",
            ignoreCase = true
        )
    ) "Others" else NA
}


fun generateFileIdentifier(): String? {
    return getBase64("" + System.currentTimeMillis() + Math.random())
}

fun getBase64(value: String): String? {
    return Base64.getEncoder().encodeToString(value.toByteArray())
}
object TempFileUtils {

    private fun log(value: String) = LoggerUtils.log("TempFileUtils.$value")

    fun safeDelete(file: File?) {
        if (file == null) return
        try {
            Files.deleteIfExists(file.toPath())
        } catch (ex: IOException) {
            log("Failed to delete file ${file.absolutePath}: ${ex.message}")
        }
    }
}

fun getLastFourDigits(aadhaarSuffix: String?): String? {
    var lastFourDigits: String? = null
    if (aadhaarSuffix != null) {
        // If length is greater than or equal to 4, get the last 4 digits
        lastFourDigits = if (aadhaarSuffix.length >= 4) {
            aadhaarSuffix.substring(aadhaarSuffix.length - 4)
        } else {
            aadhaarSuffix
        }
    }
    return lastFourDigits
}

fun sanitizeFileName(name: String): String {
    return name
        .replace(" ", "_")
        .replace(Regex("[^A-Za-z0-9_]"), "")
}
fun base64ToBinary(base64: String): ByteArray {
    val clean = base64
        .trim()
        .replace(Regex("^data:.*;base64,"), "")
        .replace("\\s".toRegex(), "")

    return Base64.getDecoder().decode(clean)
}

fun bytesToBase64(bytes: ByteArray): String {
    return Base64.getEncoder().encodeToString(bytes)
}

@Throws(Exception::class)
fun loadPrivateKey(path: String): PrivateKey {

    val inputStream = ClassPathResource(path).inputStream

    val keyString = inputStream.bufferedReader().readText()
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s+".toRegex(), "")

    val keyBytes = Base64.getDecoder().decode(keyString)
    val keySpec = PKCS8EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("RSA")

    return keyFactory.generatePrivate(keySpec)
}
