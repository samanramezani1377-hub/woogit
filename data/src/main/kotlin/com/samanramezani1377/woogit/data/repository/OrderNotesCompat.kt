package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.OrderNote
import com.samanramezani1377.woogit.data.network.WooCommerceApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private val orderNotesJson = Json { ignoreUnknownKeys = true }

/** Compatibility helper for the order-notes refresh path. */
suspend fun WooCommerceApi.orderNotes(baseUrl: String, id: Long, page: Int = 1, perPage: Int = 100): CoreResult<List<OrderNote>> {
    val response = listOrderNotes(baseUrl, id, page, perPage)
    if (response.statusCode !in 200..299) {
        return CoreResult.Failure(DomainError.Network("دریافت یادداشت‌های سفارش از فروشگاه ناموفق بود."))
    }
    return runCatching {
        val array = orderNotesJson.parseToJsonElement(response.body) as? JsonArray ?: JsonArray(emptyList())
        array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val idValue = obj["id"]?.toString()?.trim('"')?.toLongOrNull() ?: return@mapNotNull null
            val note = obj["note"]?.toString()?.trim('"') ?: return@mapNotNull null
            val customerNote = obj["customer_note"]?.toString()?.trim('"')?.toBooleanStrictOrNull() ?: false
            OrderNote(EntityId(idValue.toString()), note, customerNote)
        }
    }.fold(
        onSuccess = { CoreResult.Success(it) },
        onFailure = { CoreResult.Failure(DomainError.Network("یادداشت‌های سفارش دریافتی از فروشگاه قابل پردازش نیستند.")) }
    )
}

fun <T> CoreResult<T>.getOrNull(): T? = when (this) {
    is CoreResult.Success -> value
    is CoreResult.Failure -> null
}
