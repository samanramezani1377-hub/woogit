package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class WooGitToolExecutor(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
    private val groqMode: Boolean = false,
) {
    private val orderExecutor = WooOrderToolExecutor(dependencies, storeId)
    private var pendingImageAttachment: AiAttachment? = null

    fun consumeImageAttachment(): AiAttachment? = pendingImageAttachment.also { pendingImageAttachment = null }

    suspend fun execute(name: String, raw: String, attachments: List<AiAttachment> = emptyList()): String {
        pendingImageAttachment = null
        val a = JSONObject(raw)
        val result = when (name) {
            "orders_list", "orders_get", "orders_update_status" -> orderExecutor.execute(name, a)
            "products_list" -> {
                val page = a.optInt("page", 1).coerceAtLeast(1)
                val requested = a.optInt("perPage", if (groqMode) GROQ_PRODUCTS_PAGE_SIZE else 20).coerceAtLeast(1)
                val perPage = if (groqMode) requested.coerceIn(1, GROQ_PRODUCTS_PAGE_SIZE) else requested.coerceIn(1, 99)
                val probeSize = (perPage + 1).coerceAtMost(100)
                productsListResult(
                    dependencies.getProducts(storeId, page, probeSize, a.optString("search").takeIf { it.isNotBlank() }),
                    page,
                    perPage,
                )
            }
            "products_get" -> readResult(dependencies.getProduct(storeId, EntityId(a.getLong("id").toString())))
            "products_get_image" -> productImageResult(a)
            "products_image_add" -> addProductImage(a, attachments)
            "products_image_set_primary" -> setPrimaryProductImage(a)
            "products_image_remove" -> removeProductImage(a)
            "products_create" -> createProduct(a)
            "products_update" -> updateProduct(a)
            "products_delete" -> deleteProduct(a)
            else -> throw IllegalArgumentException("ابزار ناشناخته: $name")
        }
        return withProvenance(name, a, result)
    }

    private fun readResult(result: Any): String = JSONObject().put("ok", true).put("data", JSONObject.wrap(result)).toString()

    private fun productsListResult(result: Any, page: Int, perPage: Int): String {
        val products = result as? List<*> ?: emptyList<Any?>()
        val hasMore = products.size > perPage
        val visible = products.take(perPage)
        val data = JSONArray().apply { visible.forEach { put(JSONObject.wrap(it) ?: JSONObject.NULL) } }
        return JSONObject().put("ok", true).put("page", page).put("perPage", perPage).put("count", visible.size).put("hasMore", hasMore).put("lastPage", if (hasMore) JSONObject.NULL else page).put("endOfCollection", !hasMore).put("data", data).toString()
    }

    private suspend fun productImageResult(a: JSONObject): String {
        val id = EntityId(a.getLong("id").toString())
        val index = a.optInt("imageIndex", 0).coerceAtLeast(0)
        return when (val result = dependencies.getProduct(storeId, id)) {
            is com.samanramezani1377.woogit.core.domain.error.CoreResult.Failure -> failure(result.error.toString())
            is com.samanramezani1377.woogit.core.domain.error.CoreResult.Success -> {
                val product = result.value
                val image = product.images.getOrNull(index) ?: return failure("تصویر شماره ${index + 1} برای این محصول وجود ندارد.")
                pendingImageAttachment = runCatching { downloadImage(image.src, image.name ?: "product-image") }.getOrNull()
                if (pendingImageAttachment == null) return failure("تصویر محصول از مسیر رسانه WooGit قابل دریافت نیست.")
                JSONObject().put("ok", true).put("data", JSONObject().put("productId", product.id.value).put("imageIndex", index).put("image", JSONObject().put("id", image.id?.value ?: JSONObject.NULL).put("name", image.name ?: JSONObject.NULL).put("alt", image.alt ?: JSONObject.NULL).put("availableAsAttachment", true).put("delivery", "woogit_media_attachment"))).toString()
            }
        }
    }

    private fun downloadImage(src: String, name: String): AiAttachment {
        val c = (URL(src).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 30_000; instanceFollowRedirects = true; requestMethod = "GET" }
        return try {
            val status = c.responseCode
            if (status !in 200..299) throw IllegalStateException("تصویر محصول قابل دریافت نیست (HTTP $status).")
            val bytes = c.inputStream.use { it.readBytes() }
            require(bytes.size <= MAX_IMAGE_BYTES) { "تصویر محصول بیش از 20MB است." }
            val mime = c.contentType?.substringBefore(';')?.takeIf { it.startsWith("image/") } ?: guessMime(src)
            AiAttachment(name.ifBlank { "product-image" }, mime, bytes)
        } finally { c.disconnect() }
    }

    private fun guessMime(src: String) = when (src.substringBefore('?').substringAfterLast('.').lowercase()) { "png" -> "image/png"; "webp" -> "image/webp"; "gif" -> "image/gif"; "heic" -> "image/heic"; "heif" -> "image/heif"; else -> "image/jpeg" }

    private fun failure(message: String) = JSONObject().put("ok", false).put("verified", false).put("error", message).toString()

    private fun withProvenance(tool: String, arguments: JSONObject, result: String): String = runCatching {
        val payload = JSONObject(result)
        payload.put("provenance", JSONObject().put("source", "woogit").put("tool", tool).put("storeId", storeId.value).put("retrievedAtEpochMs", System.currentTimeMillis()).put("request", arguments))
        payload.toString()
    }.getOrDefault(result)

    private suspend fun addProductImage(a: JSONObject, attachments: List<AiAttachment>): String = failure("product image operations remain in the existing executor")
    private suspend fun setPrimaryProductImage(a: JSONObject): String = failure("product image operations remain in the existing executor")
    private suspend fun removeProductImage(a: JSONObject): String = failure("product image operations remain in the existing executor")
    private suspend fun createProduct(a: JSONObject): String = failure("product operations remain in the existing executor")
    private suspend fun updateProduct(a: JSONObject): String = failure("product operations remain in the existing executor")
    private suspend fun deleteProduct(a: JSONObject): String = failure("product operations remain in the existing executor")

    companion object {
        private const val GROQ_PRODUCTS_PAGE_SIZE = 10
        private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    }
}
