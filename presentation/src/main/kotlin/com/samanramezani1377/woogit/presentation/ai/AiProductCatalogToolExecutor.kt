package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import org.json.JSONArray
import org.json.JSONObject

/** Product catalog AI operations must use WooGit's existing presentation/domain interfaces. */
internal class AiProductCatalogToolExecutor(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
    private val groqMode: Boolean = false,
) {
    suspend fun execute(name: String, args: JSONObject): String = when (name) {
        "product_categories_list" -> categories(args)
        "products_list" -> products(args)
        else -> throw IllegalArgumentException("ابزار کاتالوگ ناشناخته: $name")
    }

    private suspend fun categories(args: JSONObject): String {
        val page = args.optInt("page", 1).coerceAtLeast(1)
        val requested = args.optInt("perPage", if (groqMode) 20 else 100).coerceAtLeast(1)
        val perPage = if (groqMode) requested.coerceIn(1, 20) else requested.coerceIn(1, 100)
        val search = args.optString("search").trim().takeIf { it.isNotBlank() }
        return when (val result = dependencies.getProductCategories(storeId, page, perPage, search)) {
            is CoreResult.Failure -> failure(result.error.toString())
            is CoreResult.Success -> {
                val data = JSONArray().apply {
                    result.value.forEach { category ->
                        put(JSONObject().put("id", category.id.value.toLongOrNull() ?: category.id.value)
                            .put("name", category.name)
                            .put("parentId", category.parentId?.value?.toLongOrNull() ?: JSONObject.NULL))
                    }
                }
                JSONObject().put("ok", true).put("page", page).put("perPage", perPage)
                    .put("count", result.value.size).put("endOfCollection", result.value.size < perPage)
                    .put("data", data).toString()
            }
        }
    }

    private suspend fun products(args: JSONObject): String {
        val categoryId = args.optLong("categoryId", 0L).takeIf { it > 0 }
            ?: return failure("برای فیلتر دسته‌بندی، categoryId معتبر لازم است.")
        val page = args.optInt("page", 1).coerceAtLeast(1)
        val requested = args.optInt("perPage", if (groqMode) 10 else 20).coerceAtLeast(1)
        val perPage = if (groqMode) requested.coerceIn(1, 10) else requested.coerceIn(1, 99)
        val probeSize = (perPage + 1).coerceAtMost(100)
        val search = args.optString("search").trim().takeIf { it.isNotBlank() }
        return when (val result = dependencies.getProducts(storeId, page, probeSize, search, categoryId)) {
            is CoreResult.Failure -> failure(result.error.toString())
            is CoreResult.Success -> {
                val hasMore = result.value.size > perPage
                val visible = result.value.take(perPage)
                JSONObject().put("ok", true).put("page", page).put("perPage", perPage)
                    .put("categoryId", categoryId).put("count", visible.size).put("hasMore", hasMore)
                    .put("lastPage", if (hasMore) JSONObject.NULL else page)
                    .put("endOfCollection", !hasMore)
                    .put("data", JSONArray().apply { visible.forEach { put(summary(it)) } }).toString()
            }
        }
    }

    private fun summary(p: Product) = JSONObject().put("id", p.id.value).put("name", p.name)
        .put("slug", p.slug ?: JSONObject.NULL).put("permalink", p.permalink ?: JSONObject.NULL)
        .put("sku", p.sku ?: JSONObject.NULL).put("status", p.status.name).put("type", p.type.name)
        .put("description", p.description ?: JSONObject.NULL).put("shortDescription", p.shortDescription ?: JSONObject.NULL)
        .put("pricing", JSONObject().put("regular", p.pricing.regular ?: JSONObject.NULL).put("sale", p.pricing.sale ?: JSONObject.NULL).put("onSale", p.pricing.onSale))
        .put("stock", p.stock?.let { JSONObject().put("quantity", it.quantity ?: JSONObject.NULL).put("status", it.status.name).put("manageStock", it.manageStock) } ?: JSONObject.NULL)
        .put("categories", JSONArray().apply { p.categories.forEach { put(JSONObject().put("id", it.id.value).put("name", it.name).put("parentId", it.parentId?.value ?: JSONObject.NULL)) } })
        .put("images", JSONArray().apply { p.images.forEachIndexed { index, image -> put(JSONObject().put("index", index).put("id", image.id?.value ?: JSONObject.NULL).put("name", image.name ?: JSONObject.NULL).put("alt", image.alt ?: JSONObject.NULL).put("available", true)) } })

    private fun failure(message: String) = JSONObject().put("ok", false).put("error", message).toString()
}