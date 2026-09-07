package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.BackendSessionStore
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.UUID

class BackendClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val credentials: SecureCredentialStore,
    private val sessions: BackendSessionStore,
    private val appVersion: String,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun verifySite(storeId: String, siteUrl: String, pair: CredentialPair): Result<BackendVerifyResult> = runCatching {
        val response = httpClient.post(url("/wp-json/woogit/v1/sites/verify")) {
            header("X-WooGit-App-Version", appVersion)
            header("Idempotency-Key", "verify-$storeId-${UUID.randomUUID()}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), kotlinx.serialization.json.buildJsonObject {
                put("url", siteUrl); put("wordpress_username", pair.wordpressUsername.orEmpty()); put("wordpress_application_password", pair.wordpressApplicationPassword.orEmpty()); put("consumer_key", pair.consumerKey); put("consumer_secret", pair.consumerSecret)
            }))
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) throw BackendHttpException(response.status.value, body)
        val obj = json.parseToJsonElement(body).jsonObject
        val token = obj["session"]?.jsonPrimitive?.contentOrNull ?: throw BackendProtocolException("Missing Backend session")
        val scope = obj["scope"]?.jsonPrimitive?.contentOrNull ?: ""
        sessions.put(storeId, token)
        BackendVerifyResult(token, scope, obj["access_enabled"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false)
    }

    suspend fun forward(storeId: String, path: String, method: String, pair: CredentialPair, query: Map<String, Any> = emptyMap(), body: String? = null): ApiResponse {
        val token = sessions.get(storeId) ?: throw BackendProtocolException("Backend session is unavailable")
        val key = if (method in setOf("POST", "PUT", "PATCH", "DELETE")) "${storeId}-${UUID.randomUUID()}" else null
        val response = httpClient.request(URLBuilder(url("/wp-json/woogit/v1/forward")).apply {
            parameters.append("path", path); query.forEach { (k, v) -> parameters.append(k, v.toString()) }
        }.build()) {
            this.method = HttpMethod.parse(method)
            header("X-WooGit-App-Version", appVersion)
            header("X-WooGit-Session", token)
            header("X-WooGit-Consumer-Key", pair.consumerKey)
            header("X-WooGit-Consumer-Secret", pair.consumerSecret)
            pair.wordpressUsername?.takeIf { it.isNotBlank() }?.let { header("X-WooGit-Wordpress-Username", it) }
            pair.wordpressApplicationPassword?.takeIf { it.isNotBlank() }?.let { header("X-WooGit-Wordpress-Application-Password", it) }
            key?.let { header("Idempotency-Key", it) }
            if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) sessions.remove(storeId)
        val headers = response.headers.entries().associate { it.key.lowercase() to it.value.joinToString(",") }
        return ApiResponse(response.status.value, text, method, path, headers)
    }

    suspend fun forwardBinary(storeId: String, path: String, method: String, pair: CredentialPair, query: Map<String, Any> = emptyMap(), bytes: ByteArray, contentType: String, fileName: String): ApiResponse {
        val token = sessions.get(storeId) ?: throw BackendProtocolException("Backend session is unavailable")
        val key = "${storeId}-${UUID.randomUUID()}"
        val response = httpClient.request(URLBuilder(url("/wp-json/woogit/v1/forward")).apply { parameters.append("path", path); query.forEach { (k,v) -> parameters.append(k, v.toString()) } }.build()) {
            this.method = HttpMethod.parse(method); header("X-WooGit-App-Version", appVersion); header("X-WooGit-Session", token); header("X-WooGit-Consumer-Key", pair.consumerKey); header("X-WooGit-Consumer-Secret", pair.consumerSecret); pair.wordpressUsername?.let { header("X-WooGit-Wordpress-Username", it) }; pair.wordpressApplicationPassword?.let { header("X-WooGit-Wordpress-Application-Password", it) }; header("Idempotency-Key", key); header(HttpHeaders.ContentDisposition, "attachment; filename=\"${fileName.substringAfterLast('/').substringAfterLast('\\')}\""); this.contentType(ContentType.parse(contentType)); setBody(bytes)
        }
        return ApiResponse(response.status.value, response.bodyAsText(), method, path, response.headers.entries().associate { it.key.lowercase() to it.value.joinToString(",") })
    }

    fun clearSession(storeId: String) = sessions.remove(storeId)
    private fun url(path: String) = baseUrl.trimEnd('/') + path
}

data class BackendVerifyResult(val session: String, val scope: String, val accessEnabled: Boolean)
class BackendHttpException(val statusCode: Int, val responseBody: String) : RuntimeException("WooGit Backend HTTP $statusCode")
class BackendProtocolException(message: String) : RuntimeException(message)
