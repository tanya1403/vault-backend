package com.vaultlink.app.utills

import org.json.JSONObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class OneResponse {

    fun getSuccessResponse(successResponse: JSONObject): ResponseEntity<String> {
        return ResponseEntity.status(200)
            .body(successResponse.toString())
    }

    fun getFailureResponse(failureResponse: JSONObject): ResponseEntity<String> {
        return ResponseEntity.status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(failureResponse.toString())
    }

    fun operationFailedResponse(message: String = DEFAULT_ERROR_MESSAGE): ResponseEntity<String> {
        return ResponseEntity
            .status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LocalResponse()
                    .setStatus(false)
                    .setMessage(message)
                    .setError(Errors.OPERATION_FAILED.value)
                    .setAction(Actions.RETRY.value)
                    .toJson()
                    .toString()
            )
    }

    val accessDeniedResponse: ResponseEntity<String>
        get() = ResponseEntity
            .status(401)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LocalResponse()
                    .setStatus(false)
                    .setMessage("Authentication failed.")
                    .setError(Errors.UNAUTHORIZED_ACCESS.value)
                    .setAction(Actions.AUTHENTICATE_AGAIN.value)
                    .toJson()
                    .toString()
            )

    val defaultFailureResponse: ResponseEntity<String>
        get() {
            return ResponseEntity
                .status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(LocalResponse().toJson().toString())
        }

    fun simpleResponse(code: Int, response: String?): ResponseEntity<String> {
        return ResponseEntity.status(code).body(response)
    }

    fun invalidData(message: String = "Invalid data."): ResponseEntity<String> {
        return ResponseEntity
            .status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LocalResponse()
                    .setStatus(false)
                    .setMessage(message)
                    .setError(Errors.INVALID_DATA.value)
                    .setAction(Actions.FIX_RETRY.value)
                    .toJson()
                    .toString()
            )
    }

    fun resourceNotFound(message: String = "Resource not found."): ResponseEntity<String> {
        return ResponseEntity
            .status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LocalResponse()
                    .setStatus(false)
                    .setMessage(message)
                    .setError(Errors.RESOURCE_NOT_FOUND.value)
                    .setAction(Actions.CANCEL.value)
                    .toJson()
                    .toString()
            )
    }

    fun serviceNotFound(message: String = "Requested service not found."): ResponseEntity<String> {
        return ResponseEntity
            .status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LocalResponse()
                    .setStatus(false)
                    .setMessage(message)
                    .setError(Errors.SERVICE_NOT_FOUND.value)
                    .setAction(Actions.CANCEL.value)
                    .toJson()
                    .toString()
            )
    }

    fun duplicateRecord(message: String = "Duplicate record."): ResponseEntity<String> {
        return ResponseEntity
            .status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LocalResponse()
                    .setStatus(false)
                    .setMessage(message)
                    .setError(Errors.DUPLICATE_RECORD.value)
                    .setAction(Actions.CANCEL.value)
                    .toJson()
                    .toString()
            )
    }

    fun invalidCredentials(
        message: String = "Invalid username or password."
    ) : ResponseEntity<String> {
            return ResponseEntity
                .status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    LocalResponse()
                        .setStatus(false)
                        .setMessage(message)
                        .setError(Errors.INVALID_CREDENTIALS.value)
                        .setAction(Actions.FIX_RETRY.value)
                        .toJson()
                        .toString()
                )
        }
}

class LocalResponse() {

    var isSuccess = false
    var message: String = DEFAULT_ERROR_MESSAGE
    var error: String = NA
    var action: String = NA
    var response: String = NA
    var statusCode: Int = -1

    constructor(json: JSONObject?) : this() {
        json ?: return
        isSuccess = json.optBoolean(SUCCESS, false)
        message = json.optString(MESSAGE, NA)
        error = json.optString(ERROR, NA)
        action = json.optString(ACTION, NA)
    }

    fun setStatus(status: Boolean): LocalResponse {
        isSuccess = status
        return this
    }

    fun setMessage(message: String): LocalResponse {
        this.message = message
        return this
    }

    fun setError(error: String): LocalResponse {
        this.error = error
        return this
    }

    fun setAction(action: String): LocalResponse {
        this.action = action
        return this
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(STATUS, if (isSuccess) SUCCESS else FAILURE)
            jsonObject.put(MESSAGE, message)
            jsonObject.put(ERROR, error)
            jsonObject.put(ACTION, action)
        } catch (e: Exception) {
            println("error while generating json object from LocalResponse: $e")
        }
        return jsonObject
    }

    fun getErrorMessage(): String {
        message = if (isNotNullOrNA(response) && response.startsWith("{")) {
            val entityResponse = JSONObject(response)
            if (!entityResponse.isNull(MESSAGE)) {
                entityResponse.optString(MESSAGE, DEFAULT_ERROR_MESSAGE)
            } else DEFAULT_ERROR_MESSAGE
        } else {
            DEFAULT_ERROR_MESSAGE
        }
        return message
    }
}

class LocalHTTPResponse {

    var isSuccess = false
    var message: String = NA
    var statusCode = -1
    var stringEntity: String = NA
    var errorMessage: String = NA
    var error: String = NA

}

class AMLLocalResponse() {

    var status: String = NA
    var message: String = NA
    var errorCode: String = NA
    var action: String = NA

    constructor(json: JSONObject?) : this() {
        json ?: return
        status = json.optString(STATUS, NA)
        message = json.optString(MESSAGE, NA)
        errorCode = json.optString(ERROR_CODE, NA)
        action = json.optString(ACTION, NA)
    }

    fun setStatus(status: String): AMLLocalResponse {
        this.status = status
        return this
    }

    fun setMessage(message: String): AMLLocalResponse {
        this.message = message
        return this
    }

    fun setErrorCode(errorCode: String): AMLLocalResponse {
        this.errorCode = errorCode
        return this
    }

    fun setAction(action: String): AMLLocalResponse {
        this.action = action
        return this
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(STATUS, status)
            jsonObject.put(MESSAGE, message)
            jsonObject.put(ERROR_CODE, errorCode)
            jsonObject.put(ACTION, action)
        } catch (e: Exception) {
            println("error while generating json object from AMLLocalResponse: $e")
        }
        return jsonObject
    }

}

class LocalCompressionHTTPResponse(
    var statusCode: Int = 0,
    var isSuccess: Boolean = false,
    var byteEntity: ByteArray? = null,
    var base64Entity: String? = null,
    var stringEntity: String? = null
)
