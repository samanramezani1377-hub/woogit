package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.BulkOrderStatusResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object BulkOrderStatusMapper {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun map(
        response: ApiResponse,
        requestedIds: List<Long>,
    ): List<BulkOrderStatusResult> {
        if (response.statusCode !in 200..299) {
            return requestedIds.map { id ->
                BulkOrderStatusResult(
                    orderId = EntityId(id.toString()),
                    succeeded = false,
                    error = response.body.ifBlank { "WooCommerce request failed (${response.statusCode})" },
                )
            }
        }

        val root = runCatching {
            json.parseToJsonElement(response.body).jsonObject
        }.getOrNull()

        val updated = root?.get("update") as? JsonArray
        val byId = updated.orEmpty().associateBy { element ->
            (element as? JsonObject)?.get("id")?.jsonPrimitive?.longOrNull
        }

        return requestedIds.map { id ->
            val item = byId[id]
            BulkOrderStatusResult(
                orderId = EntityId(id.toString()),
                succeeded = item != null,
                error = if (item == null) {
                    "WooCommerce did not return this order in the batch response"
                } else {
                    null
                },
            )
        }
    }
}
