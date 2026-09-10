package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.BackendSessionStore
import com.samanramezani1377.woogit.presentation.settings.BillingActivation
import com.samanramezani1377.woogit.presentation.settings.BillingCheckout
import com.samanramezani1377.woogit.presentation.settings.BillingGateway
import com.samanramezani1377.woogit.presentation.settings.BillingPlan
import com.samanramezani1377.woogit.presentation.settings.BillingStatus
import com.samanramezani1377.woogit.presentation.settings.BillingVariation
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import java.security.MessageDigest

class BillingClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessions: BackendSessionStore,
    private val appVersion: String,
) : BillingGateway {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun plans(storeId: StoreId): Result<List<BillingPlan>> = runCatching {
        val response = httpClient.get(url("/wp-json/woogit/v1/billing/plans")) {
            header("X-WooGit-App-Version", appVersion)
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) throw BackendHttpException(response.status.value, body, extractMessage(body))
        val root = json.parseToJsonElement(body).jsonObject
        root["plans"]?.jsonArray.orEmpty().map { it.toPlan() }
    }

    override suspend fun status(storeId: StoreId): Result<BillingStatus> = runCatching {
        val token = requireSession(storeId)
        val response = httpClient.get(url("/wp-json/woogit/v1/billing/status")) {
            header("X-WooGit-App-Version", appVersion)
            header("X-WooGit-Session", token)
        }
        val body = response.bodyAsText()
        if (response.status.value == 401) sessions.remove(storeId.value)
        if (response.status.value !in 200..299) throw BackendHttpException(response.status.value, body, extractMessage(body))
        val billing = json.parseToJsonElement(body).jsonObject["billing"]?.jsonObject ?: JsonObject(emptyMap())
        BillingStatus(
            status = billing.string("status") ?: "none",
            startsAt = billing.string("starts_at"),
            expiresAt = billing.string("expires_at"),
            capabilities = billing["capabilities"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
            trialUsed = billing["trial_used"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    override suspend fun checkout(storeId: StoreId, planId: Int, variationId: Int): Result<BillingCheckout> = runCatching {
        val token = requireSession(storeId)
        val key = "app-billing-${sha256("${storeId.value}|$planId|$variationId".toByteArray())}-${System.currentTimeMillis()}"
        val response = httpClient.post(url("/wp-json/woogit/v1/billing/checkout")) {
            header("X-WooGit-App-Version", appVersion)
            header("X-WooGit-Session", token)
            header("Idempotency-Key", key)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("plan_id", planId)
                if (variationId > 0) put("variation_id", variationId)
            })
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) throw BackendHttpException(response.status.value, body, extractMessage(body))
        val root = json.parseToJsonElement(body).jsonObject
        BillingCheckout(
            orderId = root["order_id"]?.jsonPrimitive?.intOrNull ?: 0,
            paymentUrl = root["payment_url"]?.jsonPrimitive?.contentOrNull ?: error("Missing payment_url"),
            status = root["status"]?.jsonPrimitive?.contentOrNull ?: "pending",
        )
    }

    override suspend fun activateOperationalSession(storeId: StoreId): Result<BillingActivation> = runCatching {
        val token = requireSession(storeId)
        val response = httpClient.post(url("/wp-json/woogit/v1/billing/activate-session")) {
            header("X-WooGit-App-Version", appVersion)
            header("X-WooGit-Session", token)
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) throw BackendHttpException(response.status.value, body, extractMessage(body))
        val root = json.parseToJsonElement(body).jsonObject
        val session = root["session"]?.jsonPrimitive?.contentOrNull ?: error("Missing operational session")
        val scope = root["scope"]?.jsonPrimitive?.contentOrNull ?: "operational"
        sessions.put(storeId.value, session)
        BillingActivation(session, scope, root["expires_at"]?.jsonPrimitive?.contentOrNull)
    }

    private suspend fun requireSession(storeId: StoreId): String = sessions.get(storeId.value)
        ?: throw BackendProtocolException("Backend session is unavailable")

    private fun JsonElement.toPlan(): BillingPlan {
        val obj = jsonObject
        return BillingPlan(
            id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
            key = obj.string("key").orEmpty(),
            name = obj.string("name").orEmpty(),
            price = obj.string("price").orEmpty(),
            regularPrice = obj.string("regular_price").orEmpty(),
            currency = obj.string("currency").orEmpty(),
            billingPeriod = obj.string("billing_period").orEmpty(),
            billingInterval = obj["billing_interval"]?.jsonPrimitive?.intOrNull ?: 1,
            description = obj.string("description").orEmpty(),
            type = obj.string("type").orEmpty(),
            requiresVariation = obj["requires_variation"]?.jsonPrimitive?.booleanOrNull ?: false,
            variations = obj["variations"]?.jsonArray?.map { variation ->
                val v = variation.jsonObject
                BillingVariation(
                    id = v["id"]?.jsonPrimitive?.intOrNull ?: 0,
                    name = v.string("name").orEmpty(),
                    price = v.string("price").orEmpty(),
                    regularPrice = v.string("regular_price").orEmpty(),
                )
            } ?: emptyList(),
        )
    }

    private fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull
    private fun extractMessage(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject.let { it["message"]?.jsonPrimitive?.contentOrNull ?: it["reason"]?.jsonPrimitive?.contentOrNull ?: it["code"]?.jsonPrimitive?.contentOrNull }
    }.getOrNull()
    private fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
    private fun url(path: String) = baseUrl.trimEnd('/') + path
}
