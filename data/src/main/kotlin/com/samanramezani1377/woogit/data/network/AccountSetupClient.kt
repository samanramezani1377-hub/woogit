package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.security.BackendSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AccountSetupClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessions: BackendSessionStore,
    private val appVersion: String,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun requiresWebPassword(storeId: String): CoreResult<Boolean> = runCatching {
        val token = sessions.get(storeId) ?: return@runCatching CoreResult.Failure(
            DomainError.Network("Backend session is unavailable")
        )
        val response = httpClient.get(url("/wp-json/woogit/v1/account/requirements")) {
            header("X-WooGit-App-Version", appVersion)
            header("X-WooGit-Session", token)
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            return@runCatching CoreResult.Failure(
                DomainError.Network("WooGit account requirements HTTP ${response.status.value}")
            )
        }
        val root = json.parseToJsonElement(body)
        val requirements = when (root) {
            is JsonArray -> root
            is JsonObject -> root["requirements"]?.jsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        val required = requirements.any { item ->
            val obj = item.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: ""
            val requiredFlag = obj["required"]?.jsonPrimitive?.content?.toBoolean() ?: false
            requiredFlag && (type == "1" || obj["id"]?.jsonPrimitive?.content == "web_account_password")
        }
        CoreResult.Success(required)
    }.getOrElse { throwable ->
        CoreResult.Failure(DomainError.Network("${throwable::class.simpleName}:${throwable.message.orEmpty()}"))
    }

    suspend fun setupWebPassword(
        storeId: String,
        password: String,
        confirmation: String,
    ): CoreResult<Unit> = runCatching {
        val token = sessions.get(storeId) ?: return@runCatching CoreResult.Failure(
            DomainError.Network("Backend session is unavailable")
        )
        val response = httpClient.post(url("/wp-json/woogit/v1/account/setup-web-credentials")) {
            header("X-WooGit-App-Version", appVersion)
            header("X-WooGit-Session", token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("password", password)
                put("password_confirmation", confirmation)
            })
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            return@runCatching CoreResult.Failure(
                DomainError.Network(extractMessage(body) ?: "WooGit account password setup failed")
            )
        }
        CoreResult.Success(Unit)
    }.getOrElse { throwable ->
        CoreResult.Failure(DomainError.Network("${throwable::class.simpleName}:${throwable.message.orEmpty()}"))
    }

    private fun extractMessage(body: String): String? = runCatching {
        val obj = json.parseToJsonElement(body).jsonObject
        obj["message"]?.jsonPrimitive?.content
            ?: obj["reason"]?.jsonPrimitive?.content
            ?: obj["code"]?.jsonPrimitive?.content
    }.getOrNull()

    private fun url(path: String): String = baseUrl.trimEnd('/') + path
}
