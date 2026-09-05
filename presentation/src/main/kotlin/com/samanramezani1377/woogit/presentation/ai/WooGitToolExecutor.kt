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
    private val groqMode: Boolean = false,
) {
    suspend fun execute(name: String, raw: String): String {
        val a = JSONObject(raw)
        return when (name) {
            "products_list" -> {
                val requestedPerPage = a.optInt("perPage", if (groqMode) GROQ_PRODUCTS_PAGE_SIZE else 20)
                val perPage = if (groqMode) requestedPerPage.coerceIn(1, GROQ_PRODUCTS_PAGE_SIZE) else requestedPerPage.coerceIn(1, 100)
                productsListResult(dependencies.getProducts(storeId, a.optInt("page", 1), perPage, a.optString("search").takeIf { it.isNotBlank() }), a.optInt("page", 1), perPage)
            }
            "products_get" -> readResult(dependencies.getProduct(storeId, EntityId(a.getLong("id").toString())))
            "products_create" -> createProduct(a)
            "products_update" -> updateProduct(a)
            "products_delete" -> deleteProduct(a)
            "orders_list" -> readResult(dependencies.getOrders(storeId, a.optInt("page", 1), a.optInt("perPage", 20), a.optString("search").takeIf { it.isNotBlank() }, a.optString("status").takeIf { it.isNotBlank() }))
            "orders_get" -> readResult(dependencies.getOrder(storeId, EntityId(a.getLong("id").toString())))
            "orders_update_status" -> updateOrderStatus(a)
            else -> throw IllegalArgumentException("ابزار ناشناخته: $name")
        }
    }

    private fun readResult(result: CoreResult<*>): String = when (result) {
        is CoreResult.Success -> JSONObject().put("ok", true).put("data", stringify(result.value)).toString()
        is CoreResult.Failure -> JSONObject().put("ok", false).put("error", result.error.toString()).toString()
    }

    private fun productsListResult(result: CoreResult<*>, page: Int, perPage: Int): String = when (result) {
        is CoreResult.Failure -> JSONObject().put("ok", false).put("error", result.error.toString()).toString()
        is CoreResult.Success -> {
            val products = result.value as? List<*> ?: emptyList<Any?>()
            val data = JSONArray().apply { products.forEach { value -> if (value is Product && groqMode) put(productSummaryJson(value)) else put(stringify(value)) } }
            JSONObject().put("ok", true).put("page", page).put("perPage", perPage).put("count", products.size).put("hasMore", products.size >= perPage).put("data", data).toString()
        }
    }

    private suspend fun createProduct(a: JSONObject): String {
        val requested = Product(EntityId("new"), a.getString("name"), a.optString("sku").takeIf { it.isNotBlank() }, a.optString("description").takeIf { it.isNotBlank() }, null, productStatus(a, ProductStatus.DRAFT), ProductType.SIMPLE, Pricing(a.optString("regularPrice").takeIf { it.isNotBlank() }, null, false), stockFromPatch(a, null), emptyList(), emptyList(), emptyList(), null)
        return when (val created = dependencies.createProduct(storeId, requested)) {
            is CoreResult.Failure -> failure(created.error.toString())
            is CoreResult.Success -> (created.value as? Product)?.let { verifyProduct(it, a, "ایجاد محصول") } ?: failure("ایجاد محصول انجام شد اما محصول ایجادشده قابل تأیید نبود.")
        }
    }

    private suspend fun updateProduct(a: JSONObject): String {
        val id = EntityId(a.getLong("id").toString()); val patch = a.getJSONObject("patch")
        return when (val current = dependencies.getProduct(storeId, id)) {
            is CoreResult.Failure -> failure(current.error.toString())
            is CoreResult.Success -> {
                val p = current.value
                val updated = p.copy(name = if (patch.has("name")) patch.getString("name") else p.name, sku = if (patch.has("sku")) patch.optString("sku").takeIf { it.isNotBlank() } else p.sku, description = if (patch.has("description")) patch.getString("description") else p.description, shortDescription = if (patch.has("shortDescription")) patch.getString("shortDescription") else p.shortDescription, status = productStatus(patch, p.status), pricing = p.pricing.copy(regular = if (patch.has("regularPrice")) patch.getString("regularPrice") else p.pricing.regular, sale = if (patch.has("salePrice")) patch.getString("salePrice") else p.pricing.sale, onSale = if (patch.has("salePrice")) patch.getString("salePrice").isNotBlank() else p.pricing.onSale), stock = stockFromPatch(patch, p.stock))
                when (val result = dependencies.updateProduct(storeId, id, updated)) {
                    is CoreResult.Failure -> failure(result.error.toString())
                    is CoreResult.Success -> when (val reread = dependencies.getProduct(storeId, id)) {
                        is CoreResult.Failure -> failure("تغییر محصول ارسال شد، اما وضعیت نهایی قابل تأیید نیست: ${reread.error}")
                        is CoreResult.Success -> verifyProduct(reread.value, patch, "ویرایش محصول")
                    }
                }
            }
        }
    }

    private suspend fun deleteProduct(a: JSONObject): String {
        val id = EntityId(a.getLong("id").toString())
        return when (val result = dependencies.deleteProduct(storeId, id)) {
            is CoreResult.Failure -> failure(result.error.toString())
            is CoreResult.Success -> when (val reread = dependencies.getProduct(storeId, id)) {
                is CoreResult.Success -> failure("حذف محصول گزارش شد، اما محصول هنوز از WooGit قابل دریافت است؛ عملیات موفق تأیید نشد.")
                is CoreResult.Failure -> JSONObject().put("ok", true).put("verified", true).put("operation", "products_delete").put("message", "حذف محصول با موفقیت انجام و تأیید شد.").toString()
            }
        }
    }

    private suspend fun updateOrderStatus(a: JSONObject): String {
        val id = EntityId(a.getLong("id").toString()); val requested = a.getString("status")
        return when (val current = dependencies.getOrder(storeId, id)) {
            is CoreResult.Failure -> failure(current.error.toString())
            is CoreResult.Success -> when (val result = dependencies.updateOrder(storeId, id, current.value.copy(status = OrderStatus.valueOf(requested)))) {
                is CoreResult.Failure -> failure(result.error.toString())
                is CoreResult.Success -> when (val reread = dependencies.getOrder(storeId, id)) {
                    is CoreResult.Failure -> failure("تغییر وضعیت سفارش ارسال شد، اما وضعیت نهایی قابل تأیید نیست: ${reread.error}")
                    is CoreResult.Success -> if (reread.value.status == OrderStatus.valueOf(requested)) JSONObject().put("ok", true).put("verified", true).put("operation", "orders_update_status").put("message", "وضعیت سفارش با موفقیت تغییر و تأیید شد.").toString() else failure("تغییر وضعیت سفارش تأیید نشد؛ وضعیت نهایی ${reread.value.status.name} است.")
                }
            }
        }
    }

    private fun verifyProduct(product: Product, patch: JSONObject, operation: String): String {
        val mismatches = mutableListOf<String>()
        if (patch.has("name") && product.name != patch.getString("name")) mismatches += "name"
        if (patch.has("sku") && product.sku != patch.optString("sku").takeIf { it.isNotBlank() }) mismatches += "sku"
        if (patch.has("description") && product.description != patch.getString("description")) mismatches += "description"
        if (patch.has("shortDescription") && product.shortDescription != patch.getString("shortDescription")) mismatches += "shortDescription"
        if (patch.has("regularPrice") && product.pricing.regular != patch.getString("regularPrice")) mismatches += "regularPrice"
        if (patch.has("salePrice") && product.pricing.sale != patch.getString("salePrice")) mismatches += "salePrice"
        if (patch.has("status") && product.status != productStatus(patch, product.status)) mismatches += "status"
        if (patch.has("stockQuantity") && product.stock?.quantity != patch.optDouble("stockQuantity")) mismatches += "stockQuantity"
        if (patch.has("stockStatus") && product.stock?.status != stockFromPatch(patch, product.stock)?.status) mismatches += "stockStatus"
        if (patch.has("manageStock") && product.stock?.manageStock != patch.optBoolean("manageStock")) mismatches += "manageStock"
        return if (mismatches.isEmpty()) JSONObject().put("ok", true).put("verified", true).put("operation", operation).put("data", productJson(product)).toString() else failure("$operation ارسال شد، اما مقدار نهایی با درخواست یکسان نیست: ${mismatches.joinToString(", ")}")
    }

    private fun failure(message: String) = JSONObject().put("ok", false).put("verified", false).put("error", message).toString()
    private fun productStatus(value: JSONObject, current: ProductStatus): ProductStatus { if (!value.has("status") || value.isNull("status")) return current; return when (value.optString("status").trim().lowercase()) { "publish", "published" -> ProductStatus.PUBLISHED; "draft" -> ProductStatus.DRAFT; "pending" -> ProductStatus.PENDING; "private" -> ProductStatus.PRIVATE; else -> throw IllegalArgumentException("status باید یکی از publish، draft، pending یا private باشد.") } }
    private fun stockFromPatch(patch: JSONObject, current: Stock?): Stock? { val hasQuantity = patch.has("stockQuantity") && !patch.isNull("stockQuantity"); val hasStatus = patch.has("stockStatus") && !patch.isNull("stockStatus"); val hasManage = patch.has("manageStock") && !patch.isNull("manageStock"); if (!hasQuantity && !hasStatus && !hasManage) return current; val quantity = if (hasQuantity) patch.optDouble("stockQuantity", Double.NaN).takeUnless { it.isNaN() } else current?.quantity; val status = if (hasStatus) when (patch.optString("stockStatus").trim().lowercase()) { "outofstock", "out_of_stock" -> StockStatus.OUT_OF_STOCK; "onbackorder", "on_backorder" -> StockStatus.ON_BACKORDER; "instock", "in_stock" -> StockStatus.IN_STOCK; else -> throw IllegalArgumentException("stockStatus باید یکی از instock، outofstock یا onbackorder باشد.") } else if (hasQuantity) { if ((quantity ?: 0.0) <= 0.0) StockStatus.OUT_OF_STOCK else StockStatus.IN_STOCK } else current?.status ?: StockStatus.IN_STOCK; val manageStock = if (hasManage) patch.optBoolean("manageStock") else if (hasQuantity) true else current?.manageStock ?: true; return Stock(quantity, status, manageStock) }

    private fun stringify(v: Any?): Any = when (v) { null -> JSONObject.NULL; is Product -> productJson(v); is Order -> JSONObject().put("id", v.id.value).put("number", v.number).put("status", v.status.name).put("total", v.total ?: JSONObject.NULL).put("currency", v.currency ?: JSONObject.NULL); is List<*> -> JSONArray().apply { v.forEach { put(stringify(it)) } }; else -> v.toString() }
    private fun productSummaryJson(p: Product) = JSONObject().apply { put("id", p.id.value); put("name", p.name); put("sku", p.sku ?: JSONObject.NULL); put("status", p.status.name); put("type", p.type.name); put("regularPrice", p.pricing.regular ?: JSONObject.NULL); put("salePrice", p.pricing.sale ?: JSONObject.NULL); put("stockQuantity", p.stock?.quantity ?: JSONObject.NULL); put("stockStatus", p.stock?.status?.name ?: JSONObject.NULL); put("manageStock", p.stock?.manageStock ?: JSONObject.NULL) }
    private fun productJson(p: Product) = JSONObject().apply {
        put("id", p.id.value); put("name", p.name); put("sku", p.sku ?: JSONObject.NULL)
        put("description", p.description ?: JSONObject.NULL); put("shortDescription", p.shortDescription ?: JSONObject.NULL)
        put("status", p.status.name); put("type", p.type.name)
        put("regularPrice", p.pricing.regular ?: JSONObject.NULL); put("salePrice", p.pricing.sale ?: JSONObject.NULL)
        put("stockQuantity", p.stock?.quantity ?: JSONObject.NULL); put("stockStatus", p.stock?.status?.name ?: JSONObject.NULL); put("manageStock", p.stock?.manageStock ?: JSONObject.NULL)
        put("images", JSONArray().apply { p.images.forEach { image -> put(JSONObject().put("id", image.id?.value ?: JSONObject.NULL).put("src", image.src).put("name", image.name ?: JSONObject.NULL).put("alt", image.alt ?: JSONObject.NULL)) } })
    }
    private companion object { const val GROQ_PRODUCTS_PAGE_SIZE = 10 }
}
