package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import org.json.JSONArray
import org.json.JSONObject

internal class WooGitToolExecutor(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) {
    suspend fun execute(name: String, raw: String): String {
        val a = JSONObject(raw)
        val result: CoreResult<*> = when (name) {
            "products_list" -> dependencies.getProducts(storeId, a.optInt("page", 1), a.optInt("perPage", 20), a.optString("search").takeIf { it.isNotBlank() })
            "products_get" -> dependencies.getProduct(storeId, EntityId(a.getLong("id").toString()))
            "products_create" -> dependencies.createProduct(storeId, Product(EntityId("new"), a.getString("name"), a.optString("sku").takeIf { it.isNotBlank() }, a.optString("description").takeIf { it.isNotBlank() }, null, ProductStatus.DRAFT, ProductType.SIMPLE, Pricing(a.optString("regularPrice").takeIf { it.isNotBlank() }, null, false), null, emptyList(), emptyList(), emptyList(), null))
            "products_update" -> updateProduct(a)
            "products_delete" -> dependencies.deleteProduct(storeId, EntityId(a.getLong("id").toString()))
            "orders_list" -> dependencies.getOrders(storeId, a.optInt("page", 1), a.optInt("perPage", 20), a.optString("search").takeIf { it.isNotBlank() }, a.optString("status").takeIf { it.isNotBlank() })
            "orders_get" -> dependencies.getOrder(storeId, EntityId(a.getLong("id").toString()))
            "orders_update_status" -> updateOrderStatus(a)
            else -> throw IllegalArgumentException("ابزار ناشناخته: $name")
        }
        return when (result) {
            is CoreResult.Success -> JSONObject().put("ok", true).put("data", stringify(result.value)).toString()
            is CoreResult.Failure -> JSONObject().put("ok", false).put("error", result.error.toString()).toString()
        }
    }

    private suspend fun updateProduct(a: JSONObject): CoreResult<*> {
        val id = EntityId(a.getLong("id").toString())
        return when (val current = dependencies.getProduct(storeId, id)) {
            is CoreResult.Success -> {
                val p = current.value
                val patch = a.getJSONObject("patch")
                dependencies.updateProduct(storeId, id, p.copy(
                    name = if (patch.has("name")) patch.getString("name") else p.name,
                    sku = if (patch.has("sku")) patch.optString("sku").takeIf { it.isNotBlank() } else p.sku,
                    description = if (patch.has("description")) patch.getString("description") else p.description,
                    shortDescription = if (patch.has("shortDescription")) patch.getString("shortDescription") else p.shortDescription,
                    pricing = p.pricing.copy(
                        regular = if (patch.has("regularPrice")) patch.getString("regularPrice") else p.pricing.regular,
                        sale = if (patch.has("salePrice")) patch.getString("salePrice") else p.pricing.sale,
                        onSale = if (patch.has("salePrice")) patch.getString("salePrice").isNotBlank() else p.pricing.onSale,
                    ),
                ))
            }
            is CoreResult.Failure -> current
        }
    }

    private suspend fun updateOrderStatus(a: JSONObject): CoreResult<*> {
        val id = EntityId(a.getLong("id").toString())
        return when (val current = dependencies.getOrder(storeId, id)) {
            is CoreResult.Success -> dependencies.updateOrder(storeId, id, current.value.copy(status = OrderStatus.valueOf(a.getString("status"))))
            is CoreResult.Failure -> current
        }
    }

    private fun stringify(v: Any?): Any = when (v) {
        null -> JSONObject.NULL
        is Product -> productJson(v)
        is Order -> JSONObject().put("id", v.id.value).put("number", v.number).put("status", v.status.name).put("total", v.total ?: JSONObject.NULL).put("currency", v.currency ?: JSONObject.NULL)
        is List<*> -> JSONArray().apply { v.forEach { put(stringify(it)) } }
        else -> v.toString()
    }

    private fun productJson(p: Product) = JSONObject().apply {
        put("id", p.id.value); put("name", p.name); put("sku", p.sku ?: JSONObject.NULL)
        put("status", p.status.name); put("type", p.type.name)
        put("regularPrice", p.pricing.regular ?: JSONObject.NULL); put("salePrice", p.pricing.sale ?: JSONObject.NULL)
        put("stockQuantity", p.stock?.quantity ?: JSONObject.NULL); put("stockStatus", p.stock?.status?.name ?: JSONObject.NULL)
    }
}
