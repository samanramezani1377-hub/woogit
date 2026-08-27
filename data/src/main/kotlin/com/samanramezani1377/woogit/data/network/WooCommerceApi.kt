package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.CredentialPair
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.statement.bodyAsText
import java.util.Base64

class WooCommerceApi(private val client: HttpClient, private val credentials: CredentialPair) {
    suspend fun validateStore(baseUrl: String) = request(baseUrl, "/wp-json/wc/v3/system_status")
    suspend fun listOrders(baseUrl: String, page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null) = request(baseUrl, "/wp-json/wc/v3/orders", params = params(page, perPage, search, status))
    suspend fun getOrder(baseUrl: String, id: Long) = request(baseUrl, "/wp-json/wc/v3/orders/$id")
    suspend fun updateOrder(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/orders/$id", "PUT", body)
    suspend fun addOrderNote(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/orders/$id/notes", "POST", body)
    suspend fun deleteOrder(baseUrl: String, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/orders/$id", "DELETE", params = mapOf("force" to force))
    suspend fun listProducts(baseUrl: String, page: Int = 1, perPage: Int = 20, search: String? = null) = request(baseUrl, "/wp-json/wc/v3/products", params = params(page, perPage, search))
    suspend fun getProduct(baseUrl: String, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/$id")
    suspend fun createProduct(baseUrl: String, body: String) = request(baseUrl, "/wp-json/wc/v3/products", "POST", body)
    suspend fun updateProduct(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/$id", "PUT", body)
    suspend fun deleteProduct(baseUrl: String, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/$id", "DELETE", params = mapOf("force" to force))
    suspend fun listProductCategories(baseUrl: String, page: Int = 1, perPage: Int = 100, search: String? = null) = request(baseUrl, "/wp-json/wc/v3/products/categories", params = params(page, perPage, search))
    suspend fun listVariations(baseUrl: String, productId: Long, page: Int = 1, perPage: Int = 20) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations", params = params(page, perPage))
    suspend fun getVariation(baseUrl: String, productId: Long, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations/$id")
    suspend fun createVariation(baseUrl: String, productId: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations", "POST", body)
    suspend fun updateVariation(baseUrl: String, productId: Long, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations/$id", "PUT", body)
    suspend fun deleteVariation(baseUrl: String, productId: Long, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations/$id", "DELETE", params = mapOf("force" to force))
    suspend fun listAttributes(baseUrl: String, page: Int = 1, perPage: Int = 100) = request(baseUrl, "/wp-json/wc/v3/products/attributes", params = params(page, perPage))
    suspend fun getAttribute(baseUrl: String, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$id")
    suspend fun createAttribute(baseUrl: String, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes", "POST", body)
    suspend fun updateAttribute(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$id", "PUT", body)
    suspend fun deleteAttribute(baseUrl: String, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$id", "DELETE", params = mapOf("force" to force))
    suspend fun listAttributeTerms(baseUrl: String, attributeId: Long, page: Int = 1, perPage: Int = 100) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms", params = params(page, perPage))
    suspend fun getAttributeTerm(baseUrl: String, attributeId: Long, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms/$id")
    suspend fun createAttributeTerm(baseUrl: String, attributeId: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms", "POST", body)
    suspend fun updateAttributeTerm(baseUrl: String, attributeId: Long, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms/$id", "PUT", body)
    suspend fun deleteAttributeTerm(baseUrl: String, attributeId: Long, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms/$id", "DELETE", params = mapOf("force" to force))
    suspend fun uploadMedia(baseUrl: String, fileName: String, bytes: ByteArray, mediaType: String) = requestBytes(baseUrl, "/wp-json/wp/v2/media", fileName, bytes, mediaType)
    suspend fun deleteMedia(baseUrl: String, mediaId: Long, force: Boolean = true) = request(baseUrl, "/wp-json/wp/v2/media/$mediaId", "DELETE", params = mapOf("force" to force))

    private suspend fun request(baseUrl: String, path: String, method: String = "GET", body: String? = null, params: Map<String, Any> = emptyMap()): ApiResponse {
        val url = Url("${baseUrl.trimEnd('/')}$path")
        require(url.protocol.name == "https") { "WooCommerce API requires HTTPS" }
        val response = when (method) { "POST" -> client.post(url) { common(params); contentType(ContentType.Application.Json); setBody(body ?: "{}") }; "PUT" -> client.put(url) { common(params); contentType(ContentType.Application.Json); setBody(body ?: "{}") }; "PATCH" -> client.patch(url) { common(params); contentType(ContentType.Application.Json); setBody(body ?: "{}") }; "DELETE" -> client.delete(url) { common(params) }; else -> client.get(url) { common(params) } }
        return ApiResponse(response.status.value, response.bodyAsText())
    }
    private suspend fun requestBytes(baseUrl: String, path: String, fileName: String, bytes: ByteArray, mediaType: String): ApiResponse {
        val url = Url("${baseUrl.trimEnd('/')}$path")
        require(url.protocol.name == "https") { "WooCommerce API requires HTTPS" }
        val response = client.post(url) { header(HttpHeaders.Authorization, basicAuth()); header(HttpHeaders.ContentDisposition, "attachment; filename=\"$fileName\""); header(HttpHeaders.ContentType, mediaType); setBody(bytes) }
        if (response.status.value != 401) return ApiResponse(response.status.value, response.bodyAsText())
        val retry = client.post(url) { parameter("consumer_key", credentials.consumerKey); parameter("consumer_secret", credentials.consumerSecret); header(HttpHeaders.ContentDisposition, "attachment; filename=\"$fileName\""); header(HttpHeaders.ContentType, mediaType); setBody(bytes) }
        return ApiResponse(retry.status.value, retry.bodyAsText())
    }
    private fun HttpRequestBuilder.common(params: Map<String, Any>) { header(HttpHeaders.Authorization, basicAuth()); params.forEach { (key, value) -> parameter(key, value) } }
    private fun basicAuth(): String = "Basic ${Base64.getEncoder().encodeToString("${credentials.consumerKey}:${credentials.consumerSecret}".toByteArray())}"
    private fun params(page: Int, perPage: Int, search: String? = null, status: String? = null) = buildMap<String, Any> { put("page", page); put("per_page", perPage); if (!search.isNullOrBlank()) put("search", search); if (!status.isNullOrBlank()) put("status", status) }
}

data class ApiResponse(val statusCode: Int, val body: String)
