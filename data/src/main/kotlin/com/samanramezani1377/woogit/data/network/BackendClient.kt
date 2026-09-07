package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.BackendSessionStore
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.security.MessageDigest
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
            setBody(json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), buildJsonObject {
                put("url", siteUrl)
                put("wordpress_username", pair.wordpressUsername.orEmpty())
                put("wordpress_application_password", pair.wordpressApplicationPassword.orEmpty())
                put("consumer_key", pair.consumerKey)
                put("consumer_secret", pair.consumerSecret)
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

    suspend fun forward(storeId: String, path: String, method: String, pair: CredentialPair, query: Map<String, Any> = emptyMap(), body: String? = null, idempotencyKey: String? = null): ApiResponse {
        val token = sessions.get(storeId) ?: throw BackendProtocolException("Backend session is unavailable")
        val normalizedMethod = method.uppercase()
        val key = if (normalizedMethod in MUTATION_METHODS) idempotencyKey ?: stableMutationKey(storeId, normalizedMethod, path, query, body) else null
        val response = httpClient.request(URLBuilder(url("/wp-json/woogit/v1/forward")).apply { parameters.append("path", path); query.forEach { (k, v) -> parameters.append(k, v.toString()) } }.build()) {
            this.method = HttpMethod.parse(normalizedMethod)
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
        return ApiResponse(response.status.value, text, normalizedMethod, path, headers)
    }

    suspend fun forwardBinary(storeId: String, path: String, method: String, pair: CredentialPair, query: Map<String, Any> = emptyMap(), bytes: ByteArray, contentType: String, fileName: String, idempotencyKey: String? = null): ApiResponse {
        val token = sessions.get(storeId) ?: throw BackendProtocolException("Backend session is unavailable")
        val normalizedMethod = method.uppercase(); require(normalizedMethod in MUTATION_METHODS) { "Binary forwarding is only supported for mutations" }
        val key = idempotencyKey ?: stableMutationKey(storeId, normalizedMethod, path, query, "binary:$fileName:${bytes.size}")
        val response = httpClient.request(URLBuilder(url("/wp-json/woogit/v1/forward")).apply { parameters.append("path", path); query.forEach { (k, v) -> parameters.append(k, v.toString()) } }.build()) {
            this.method = HttpMethod.parse(normalizedMethod)
            header("X-WooGit-App-Version", appVersion); header("X-WooGit-Session", token)
            header("X-WooGit-Consumer-Key", pair.consumerKey); header("X-WooGit-Consumer-Secret", pair.consumerSecret)
            pair.wordpressUsername?.takeIf { it.isNotBlank() }?.let { header("X-WooGit-Wordpress-Username", it) }
            pair.wordpressApplicationPassword?.takeIf { it.isNotBlank() }?.let { header("X-WooGit-Wordpress-Application-Password", it) }
            header("Idempotency-Key", key); header(HttpHeaders.ContentDisposition, "attachment; filename=\"${fileName.substringAfterLast('/').substringAfterLast('\\')}\"")
            this.contentType(ContentType.parse(contentType)); setBody(bytes)
        }
        val text = response.bodyAsText(); if (response.status.value == 401) sessions.remove(storeId)
        return ApiResponse(response.status.value, text, normalizedMethod, path, response.headers.entries().associate { it.key.lowercase() to it.value.joinToString(",") })
    }

    suspend fun getOperation(storeId: String, operationId: String): BackendOperationStatus = runCatching {
        val token = sessions.get(storeId) ?: throw BackendProtocolException("Backend session is unavailable")
        val response = httpClient.get(url("/wp-json/woogit/v1/operations/${operationId.urlEncode()}").replace("%2F", "/")) {
            header("X-WooGit-App-Version", appVersion); header("X-WooGit-Session", token)
        }
        val body = response.bodyAsText(); if (response.status.value == 401) sessions.remove(storeId)
        if (response.status.value !in 200..299) throw BackendHttpException(response.status.value, body)
        val obj = json.parseToJsonElement(body).jsonObject
        BackendOperationStatus(
            operationId = obj["operation_id"]?.jsonPrimitive?.contentOrNull ?: operationId,
            status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "unknown",
            responseBody = obj["response"]?.toString(),
        )
    }.getOrThrow()

    suspend fun revokeSession(storeId: String): Result<Unit> = runCatching {
        val token = sessions.get(storeId) ?: return@runCatching Unit
        val response = httpClient.post(url("/wp-json/woogit/v1/sessions/revoke")) { header("X-WooGit-App-Version", appVersion); header("X-WooGit-Session", token) }
        val body = response.bodyAsText(); if (response.status.value !in 200..299 && response.status.value != 401) throw BackendHttpException(response.status.value, body)
        sessions.remove(storeId)
    }

    fun clearSession(storeId: String) = sessions.remove(storeId)

    private fun stableMutationKey(storeId: String, method: String, path: String, query: Map<String, Any>, body: String?): String {
        val canonicalQuery = query.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }; val canonical = "$storeId|$method|$path|$canonicalQuery|${body.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }; return "app-$digest"
    }
    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
    private fun url(path: String) = baseUrl.trimEnd('/') + path
    private companion object { val MUTATION_METHODS = setOf("POST", "PUT", "PATCH", "DELETE") }
}

data class BackendVerifyResult(val session: String, val scope: String, val accessEnabled: Boolean)
data class BackendOperationStatus(val operationId: String, val status: String, val responseBody: String?)
class BackendHttpException(val statusCode: Int, val responseBody: String) : RuntimeException("WooGit Backend HTTP $statusCode")
class BackendProtocolException(message: String) : RuntimeException(message)
