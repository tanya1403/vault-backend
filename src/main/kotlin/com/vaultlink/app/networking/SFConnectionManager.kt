package com.vaultlink.app.networking

import com.fasterxml.jackson.databind.ObjectMapper
import com.vaultlink.app.security.AppProperty
import com.vaultlink.app.utills.*
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.apache.http.Header
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.*
import java.net.URLEncoder
import java.util.*

const val SO_OBJECT = "sobjects"

enum class SFObjects(val value: String) {
    CONTACT("/$SO_OBJECT/Contact/"),
    CASE("/$SO_OBJECT/Case/"),
    PROPERTY_INSIGHT("/sobjects/Property_Insight__c/"),
    CONTENT_VERSION("/sobjects/ContentVersion/"),
    CONTENT_DOCUMENT_LINK("/sobjects/ContentDocumentLink/"),
    ESTAMP("/sobjects/e_Stamp_Denominations__c/"),
    DOCUMENT_PICKUP("/sobjects/Documents_Pickup__c/"),
    SERVICE_AND_DISBURSAL("/sobjects/Service_and_Disbursal__c/");

}

enum class EnSfObjectName(val value: String) {
    COLLECTION("Collection__c"),
    LOAN_ACCOUNT("loan__Loan_Account__c");
}

@Component
class SFConnection(
    @Autowired val cryptoUtils: CryptoUtils,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val appProperty: AppProperty
) {

    companion object {

        private const val GRANT_SERVICE = "/services/oauth2/token?grant_type=password"
        private const val REST_ENDPOINT = "/services/data"
        private const val APEX_REST_ENDPOINT = "/services/apexrest"
        private const val API_VERSION = "/v50.0"


    }

    var baseUri: String? = null
    private var instanceUri: String? = null
    var apexBaseUri: String? = null
    private var oauthHeader: Header? = null
    private val prettyPrintHeader: Header = BasicHeader("X-PrettyPrint", "1")
    private var retryCount = 0

    private fun log(value: String) {
        LoggerUtils.log("SFConnection.$value")
    }

    @Throws(UnsupportedEncodingException::class)
    private fun getModifiedQuery(query: String): String? {
        return URLEncoder.encode(query, "UTF-8")
    }

    private fun getBody(inputStream: InputStream): String? {
        var result: String? = ""
        try {
            val `in` = BufferedReader(InputStreamReader(inputStream))
            var inputLine: String?
            while (`in`.readLine().also { inputLine = it } != null) {
                result += inputLine
                result += "\n"
            }
            `in`.close()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
        return result
    }

    @Throws(Exception::class)
    private fun checkAndAuthenticate() {
        if (null == baseUri || null == oauthHeader) authenticate()
    }


    @Throws(Exception::class)
    private fun authenticate() {

        val privateKey = loadPrivateKey("salesforceKey/server.key")

        val now = System.currentTimeMillis()

        val jwt = Jwts.builder()
            .setIssuer(appProperty.sfClientID)
            .setSubject(appProperty.sfUserName)
            .setAudience(appProperty.sfURL)
            .setIssuedAt(Date(now))
            .setExpiration(Date(now + 3 * 60 * 1000))   // 3 minutes
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact()

        // 3️⃣ Prepare request
        val restTemplate = org.springframework.web.client.RestTemplate()
        val headers = org.springframework.http.HttpHeaders().apply {
            contentType = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
        }
        val body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
        val request = org.springframework.http.HttpEntity(body, headers)
        val tokenUrl = "${appProperty.sfURL}/services/oauth2/token"

        // 4️⃣ Make POST request
        val response = restTemplate.postForEntity(tokenUrl, request, Map::class.java)

        // 5️⃣ Parse response safely
        if (response.statusCode == org.springframework.http.HttpStatus.OK && response.body != null) {
            val tokenResponse = response.body as Map<*, *>
            val accessToken = tokenResponse["access_token"]?.toString()
                ?: throw Exception("access_token not found in response")
            val instanceUrl = tokenResponse["instance_url"]?.toString()
                ?: throw Exception("instance_url not found in response")

            instanceUri = instanceUrl
            baseUri = "$instanceUrl$REST_ENDPOINT$API_VERSION"
            apexBaseUri = "$instanceUrl$APEX_REST_ENDPOINT"
            oauthHeader = BasicHeader("Authorization", "Bearer $accessToken")

            log("JWT Auth successful, token acquired")
            log("  instance URL: $instanceUrl")
            log("  accessToken : $accessToken")
            log("baseUri: $baseUri")

        } else {
            val errorBody = response.body?.toString() ?: "No response body"
            throw Exception("Failed to get Salesforce token via JWT: $errorBody")
        }
    }


    @Throws(Exception::class)
    fun get(query: String): JSONObject? {

        return try {

            checkAndAuthenticate()

            log("get - Salesforce query: $query")
            // Set up the HTTP objects needed to make the request.
            val httpClient: HttpClient = HttpClientBuilder.create().build()
            val uri = baseUri + "/query?q=" + getModifiedQuery(query)
            println("Query URL: $uri")
            val httpGet = HttpGet(uri)
            // System.out.println("oauthHeader2: " + oauthHeader);
            httpGet.addHeader(oauthHeader)
            httpGet.addHeader(prettyPrintHeader)
            // Make the request.
            val response = httpClient.execute(httpGet)
            // Process the result
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                retryCount = 0
                val responseString = EntityUtils.toString(response.entity)
                try {
                    val json = JSONObject(responseString)
                    println("JSON result of Query:\n$json")
                    json
                } catch (je: JSONException) {
                    je.printStackTrace()
                    null
                }
            } else if (statusCode == 401 && retryCount < 3) {
                println("Query was unsuccessful. Access token was expired: $statusCode")
                baseUri = null
                oauthHeader = null
                retryCount++
                authenticate()
                get(query)
            } else {
                retryCount = 0
                println("Query was unsuccessful. Status code returned is $statusCode")
                println("An error has occured. Http status: " + response.statusLine.statusCode)
                println(getBody(response.entity.content))
                // System.exit(-1);
                null
            }
        } catch (e: java.lang.Exception) {
            log("get - error while getting data from salesforce: $e")
            e.printStackTrace()
            throw e
        }
    }

    fun apexGet(uri: String): JSONObject? {

        return try {

            checkAndAuthenticate()


            val endPoint = apexBaseUri.plus(uri)
            log("apexGet - query url : $endPoint")

            val httpClient: HttpClient = HttpClientBuilder.create().build()
            val httpGet = HttpGet(endPoint)
            httpGet.addHeader(oauthHeader)
            httpGet.addHeader(prettyPrintHeader)
            val response = httpClient.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                retryCount = 0
                val responseString = EntityUtils.toString(response.entity)
                println("apexGet  response: $responseString")
                try {
                    val json = JSONObject(responseString)
                    println("JSON result of Query:\n$json")
                    json
                } catch (je: JSONException) {
                    je.printStackTrace()
                    null
                }
            } else if (statusCode == 401 && retryCount < 3) {
                println("Query was unsuccessful. Access token was expired: $statusCode")
                baseUri = null
                oauthHeader = null
                retryCount++
                authenticate()
                apexGet(uri)
            } else {
                retryCount = 0
                println("Query was unsuccessful. Status code returned is $statusCode")
                println("An error has occurred. Http status: " + response.statusLine.statusCode)
                println(getBody(response.entity.content))
                // System.exit(-1);
                null
            }
        } catch (e: Exception) {
            log("apexGet - Error : " + e.message)
            e.printStackTrace()
            null
        }
    }

    @Throws(java.lang.Exception::class)
    fun post(requestObject: JSONObject, sfObject: SFObjects): LocalHTTPResponse {

        checkAndAuthenticate()

        val lhResponse = LocalHTTPResponse()
        val uri = baseUri + sfObject.value
        val httpClient: HttpClient = HttpClientBuilder.create().build()
        val httpPost = HttpPost(uri)
        httpPost.addHeader(oauthHeader)
        httpPost.addHeader(prettyPrintHeader)
        val body = StringEntity(requestObject.toString(1))
        body.setContentType(CONTENT_TYPE_APPLICATION_JSON)
        httpPost.entity = body
        val response = httpClient.execute(httpPost)
        val statusCode = response.statusLine.statusCode
        if (statusCode == 201) {
            retryCount = 0
            val responseString = EntityUtils.toString(response.entity)
            println("POST - Response: $responseString")
            lhResponse.isSuccess = true
            lhResponse.statusCode = statusCode
            lhResponse.stringEntity = responseString
        } else if (statusCode == 401 && retryCount < 3) {
            println("POST - Call was unsuccessful. Access token was expired: $statusCode")
            baseUri = null
            oauthHeader = null
            retryCount++
            authenticate()
            return post(requestObject, sfObject)
        } else {
            retryCount = 0
            log("POST - Call unsuccessful. Status code returned is $statusCode | error: ${response.statusLine}")
            val respArray = JSONArray(getBody(response.entity.content))
            val respObj = respArray.get(0) as JSONObject
            val errorCode = respObj.optString("errorCode", NA)
            log("POST - Error : $errorCode | Response: $respObj")
            lhResponse.isSuccess = false
            lhResponse.statusCode = statusCode
            lhResponse.message = errorCode
            lhResponse.errorMessage = respObj.toString()

        }
        return lhResponse
    }

    @Throws(java.lang.Exception::class)
    fun  patch(requestObject: JSONObject, endpoint: String?): LocalHTTPResponse? {

        checkAndAuthenticate()

        val lhResponse = LocalHTTPResponse()
        val httpClient: HttpClient = HttpClientBuilder.create().build()
        val uri = baseUri + endpoint

        val httpPatch = HttpPatch(uri)
        httpPatch.addHeader(oauthHeader)
        httpPatch.addHeader(prettyPrintHeader)
        val body = StringEntity(requestObject.toString(1))
        body.setContentType(CONTENT_TYPE_APPLICATION_JSON)
        httpPatch.entity = body
        val response = httpClient.execute(httpPatch)
        val statusCode = response.statusLine.statusCode
        if (statusCode == 204) {
            retryCount = 0
            lhResponse.isSuccess = true
            lhResponse.statusCode = statusCode
            lhResponse.stringEntity = NA
        } else if (statusCode == 401 && retryCount < 3) {
            println("PATCH Call was unsuccessful. Access token was expired: $statusCode")
            baseUri = null
            oauthHeader = null
            retryCount++
            authenticate()
            return patch(requestObject, endpoint)
        } else {
            retryCount = 0
            log("PATCH - Call unsuccessful. Status code returned is  $statusCode  | error: ${response.statusLine}")
            val respArray = JSONArray(getBody(response.entity.content))
            val respObj = respArray.get(0) as JSONObject
            val errorCode = respObj.optString("errorCode", NA)
            log("PATCH - Call Error : $errorCode | Response: $respObj")
            lhResponse.isSuccess = false
            lhResponse.statusCode = statusCode
            lhResponse.message = errorCode
        }
        return lhResponse
    }

    fun getNextRecords(nextRecordsUrl: String): JSONObject? {
        try {

            val httpClient: HttpClient = HttpClientBuilder.create().build()

            val uri = instanceUri + nextRecordsUrl
            val httpGet = HttpGet(uri)
            httpGet.addHeader(oauthHeader)
            httpGet.addHeader(prettyPrintHeader)
            val response = httpClient.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                retryCount = 0
                val responseString = EntityUtils.toString(response.entity)
                return try {
                    val json = JSONObject(responseString)
                    println("JSON result of More Query:\n$json")
                    json
                } catch (je: JSONException) {
                    je.printStackTrace()
                    null
                }
            } else if (statusCode == 401 && retryCount < 3) {
                println("More Query was unsuccessful. Access token was expired: $statusCode")
                baseUri = null
                oauthHeader = null
                retryCount++
                authenticate()
                return getNextRecords(nextRecordsUrl)
            } else {
                retryCount = 0
                println("More Query was unsuccessful. Status code returned is $statusCode")
                println("An error has occurred. Http status: " + response.statusLine.statusCode)
                println(getBody(response.entity.content))
                // System.exit(-1);
                return null
            }
        } catch (e: java.lang.Exception) {
            log("getNextRecords - Error while getting More data from salesforce: $e")
            e.printStackTrace()
            return null
        }
    }

    fun getAttachmentData(documentId: String): String? {

        var documentData: String? = null

        try {

            val httpClient: HttpClient = HttpClientBuilder.create().build()
            val uri = "$baseUri/sobjects/Attachment/$documentId/Body"
            println("Query URL: $uri")
            val httpGet = HttpGet(uri)
            println("oauthHeader2: $oauthHeader")
            httpGet.addHeader(oauthHeader)
            httpGet.addHeader(prettyPrintHeader)
            val response = httpClient.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                retryCount = 0
                try {

//                    val encoded: ByteArray = Base64.getEncoder().encode(IOUtils.toByteArray(response.entity.content))
//                    documentData = String(encoded)

                    documentData = cryptoUtils.encodeBase64(response.entity.content)

                } catch (je: JSONException) {
                    je.printStackTrace()
                    return null
                }
            } else if (statusCode == 401 && retryCount < 3) {
                println("Query was unsuccessful. Access token was expired: $statusCode")
                baseUri = null
                oauthHeader = null
                retryCount++
                authenticate()
                return getAttachmentData(documentId)
            } else {
                retryCount = 0
                println("Query was unsuccessful. Status code returned is $statusCode")
                println("An error has occurred. Http status: " + response.statusLine.statusCode)
                println(getBody(response.entity.content))
                // System.exit(-1);
                return null
            }
        } catch (e: java.lang.Exception) {
            log("getAttachmentData - Failed to get attachment for for id: $documentId")
            e.printStackTrace()
        }
        return documentData
    }

    @Throws(Exception::class)
    fun uploadContentDocumentOnSF(fileBase64: String, fileName: String?, parentId: String?): LocalResponse {

        try {

            val jsonObject = JSONObject().apply {
                put("Title", fileName)
                put("PathOnClient", fileName)
                put("VersionData", fileBase64)
            }

            val sfResponse = post(jsonObject, SFObjects.CONTENT_VERSION)

            if (!sfResponse.isSuccess) {
                log("uploadContentDocumentOnSF - Error: ${objectMapper.writeValueAsString(sfResponse)}")
                return LocalResponse().apply {
                    this.message = "Failed to upload file"
                }
            }

            val contentVersionId = JSONObject(sfResponse.stringEntity).optString("id")

            if (contentVersionId.isInvalid()) {
                log("uploadContentDocumentOnSF - Invalid content version Id")
                return LocalResponse().apply {
                    this.message = "Failed to upload file"
                }
            }

            val contentDocResponse = get("SELECT ContentDocumentId FROM ContentVersion WHERE Id = '$contentVersionId'")

            val contentDocumentId = contentDocResponse?.takeIf { it.getInt("totalSize") > 0 }
                ?.getJSONArray("records")?.getJSONObject(0)?.optString("ContentDocumentId")
                ?: run {
                    log("uploadContentDocumentOnSF - Failed to retrieve content document id")
                    return LocalResponse().apply {
                        this.message = "Failed to upload file"
                    }
                }

            val linkJson = JSONObject().apply {
                put("ContentDocumentId", contentDocumentId)
                put("LinkedEntityId", parentId)
                put("ShareType", "V")
            }

            val linkJsonResponse = post(linkJson, SFObjects.CONTENT_DOCUMENT_LINK)

            if (!linkJsonResponse.isSuccess)
                log("uploadContentDocumentOnSF - Error: ${objectMapper.writeValueAsString(linkJsonResponse)}")

            return LocalResponse().apply {
                isSuccess = linkJsonResponse.isSuccess
                message = if (linkJsonResponse.isSuccess) "File uploaded" else "Failed to upload file"
            }

        } catch (e: Exception) {
            log("uploadContentDocumentOnSF - Exception: ${e.message}")
            return LocalResponse().apply {
                this.message = "Exception occurred: ${e.message}"
            }
        }

    }

}