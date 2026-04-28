package com.vaultlink.app.utills

import org.apache.http.entity.ContentType


const val NA = "NA"
const val STATUS = "status"
const val SUCCESS = "success"
const val FAILURE = "failure"
const val MESSAGE = "message"
const val ERROR_CODE = "error code"

const val ERROR = "error"
const val ACTION = "action"

const val DEFAULT_ERROR_MESSAGE = "Something went wrong.Please try again!"

const val CONTENT_TYPE_APPLICATION_JSON = "application/json"

const val NO_DECIMAL_NUMBER = -1.0
const val NO_NUMBER = -1

const val THREAD_POOL_TASK_EXECUTOR = "threadPoolTaskExecutor"

const val SANJAY_EMAIL_ID = "sanjay.jaiswar@homefirstindia.com"
const val KAINAAT_EMAIL_ID = "kainaat.zaidi@homefirstindia.com"
const val TANYA_EMAIL_ID = "tanya.khurana@homefirstindia.com"

const val CHECK_IN = "checkIn"
const val CHECK_OUT = "checkOut"

const val CHECK_IN_DETAILS = "checkInDetails"
const val TEAM_CALLER_INFO = "teamCallerInfo"

const val PROPERTY = "property"
const val INCOME_DETAILS = "incomeDetails"

const val PROPERTY_UNIDENTIFIED = "Unidentified"
const val AGENTS = "agents"

const val ENCRYPT_DECRYPT_REQUEST_KEY = "data"

const val CUSTOMER_PORTAL_ORG_ID = "homefirst_cp"
const val MAX_DAY_RANGE = 30
const val PROGRESSIVE_PREPARATION_TIME = 10L

const val  CAPTCHA_URL="https://google.com/recaptcha/api/siteverify"
const val CAPTCHA_SECRET_KEY ="6Lcf_6opAAAAALjsrLI5jZijaCG5IXCApLgDbqeB"
const val PASSWORD_EXPIRE_DAY_COUNT = 60
const val DEFAULT_LOGIN_ATTEMPTS = 4
const val DEFAULT_LOGIN_ATTEMPT_MINUTES = 30

enum class Actions(val value: String) {
    AUTHENTICATE_AGAIN("AUTHENTICATE_AGAIN"),
    RETRY("RETRY"),
    FIX_RETRY("FIX_RETRY"),
    CANCEL("CANCEL"),
    CONTACT_ADMIN("CONTACT_ADMIN"),
    DO_REGISTRATION("DO_REGISTRATION"),
    DO_PASSWORD_RESET("DO_PASSWORD_RESET"),
    VERIFY_OTP("VERIFY_OTP");
}

enum class Errors(val value: String) {
    UNKNOWN("UNKNOWN"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    ACCESS_DENIED("ACCESS_DENIED"),
    UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS"),
    DUPLICATE_RECORD("DUPLICATE_RECORD"),
    STRING_TOO_LONG("STRING_TOO_LONG"),
    BODY_TOO_LONG("BODY_TOO_LONG"),
    JSON_PARSER_ERROR("JSON_PARSER_ERROR"),
    OPERATION_FAILED("OPERATION_FAILED"),
    INVALID_DATA("INVALID_DATA"),
    SERVICE_NOT_FOUND("SERVICE_NOT_FOUND"),
    INVALID_REQUEST("INVALID_REQUEST");
}

enum class FileTypesExtentions(val ext: String, val contentType: String, val displayName: String) {
    PDF(".pdf", "application/pdf", "PDF"),
    HTML(".html", "text/html", "HTML"),
    MP3(".mp3", "audio/mpeg", "MP3"),
    CSV(".csv", "text/csv", "CSV"),
    IMAGE(".jpeg", "image/jpeg", "Image");

    companion object {
        fun getExtFromType(ext: String): String? {
            for (item: FileTypesExtentions in values()) {
                if ((item.contentType == ext)) return item.ext
            }
            return null
        }
        operator fun get(value: String): FileTypesExtentions? {
            for (ext: FileTypesExtentions in FileTypesExtentions.values()) {
                if ((ext.ext == value)) return ext
            }
            return null
        }
        fun imageFormats() = listOf(
            ContentType.IMAGE_JPEG.mimeType,
            ContentType.IMAGE_PNG.mimeType
        )
    }
}

enum class SFObjects(val value: String) {
    DOCUMENT_PICKUP("/sobjects/Documents_Pickup__c"),
    OPPORTUNITY("/sobjects/Opportunity/"),
    CONTACT("/sobjects/Contact/"),
    APPLICATION_ID("/sobjects/ApplicationId__c/"),
    CONNECTOR("/sobjects/Connector__c/"),
    ATTACHMENT_DOC("/sobjects/Attachment/"),
    CONTENT_VERSION("/sobjects/ContentVersion/"),
    CONTENT_DOCUMENT_LINK("/sobjects/ContentDocumentLink/"),
    COLLECTIONS("/sobjects/Collection__c/")
}
