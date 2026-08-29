package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.debug.NoOpTechnicalErrorReporter
import com.samanramezani1377.woogit.core.debug.TechnicalErrorContext
import com.samanramezani1377.woogit.core.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.core.security.CredentialPair
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import java.util.Base64

class WooCommerceApi(
    private val client: HttpClient,
    private val credentials: CredentialPair,
    private val technicalErrorReporter: TechnicalErrorReporter = NoOpTechnicalErrorReporter,
) {
    suspend fun validateStore(baseUrl: String) = request(baseUrl, "/wp-json/wc/v3/system_status")
    suspend fun listOrders(baseUrl: String, page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null) = request(baseUrl, "/wp-json/wc/v3/orders", params(page, perPage, search, status))
    suspend fun getOrder(baseUrl: String, id: Long) = request(baseUrl, "/wp-json/wc/v3/orders/$id")
    suspend fun updateOrder(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/orders/$id", method = "PUT", body = body)
    suspend fun addOrderNote(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/orders/$id/notes", method = "POST", body = body)
    suspend fun deleteOrder(baseUrl: String, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/orders/$id", params = params(force), method = "DELETE")
    suspend fun salesReport(baseUrl: String, dateMin: String, dateMax: String) = request(baseUrl, "/wp-json/wc/v3/reports/sales", mapOf("date_min" to dateMin, "date_max" to dateMax))
    suspend fun listProducts(baseUrl: String, page: Int = 1, perPage: Int = 20, search: String? = null, modifiedAfter: String? = null) = request(baseUrl, "/wp-json/wc/v3/products", params(page, perPage, search, null, modifiedAfter))
    suspend fun getProduct(baseUrl: String, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/$id")
    suspend fun createProduct(baseUrl: String, body: String) = request(baseUrl, "/wp-json/wc/v3/products", method = "POST", body = body)
    suspend fun updateProduct(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/$id", method = "PUT", body = body)
    suspend fun deleteProduct(baseUrl: String, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/$id", params = params(force), method = "DELETE")
    suspend fun listProductCategories(baseUrl: String, page: Int = 1, perPage: Int = 100, search: String? = null) = request(baseUrl, "/wp-json/wc/v3/products/categories", params = params(page, perPage, search))
    suspend fun listVariations(baseUrl: String, productId: Long, page: Int = 1, perPage: Int = 20) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations", params = params(page, perPage))
    suspend fun getVariation(baseUrl: String, productId: Long, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations/$id")
    suspend fun createVariation(baseUrl: String, productId: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations", method = "POST", body = body)
    suspend fun updateVariation(baseUrl: String, productId: Long, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations/$id", method = "PUT", body = body)
    suspend fun deleteVariation(baseUrl: String, productId: Long, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/$productId/variations/$id", params = params(force), method = "DELETE")
    suspend fun listAttributes(baseUrl: String, page: Int = 1, perPage: Int = 100) = request(baseUrl, "/wp-json/wc/v3/products/attributes", params = params(page, perPage))
    suspend fun getAttribute(baseUrl: String, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$id")
    suspend fun createAttribute(baseUrl: String, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes", method = "POST", body = body)
    suspend fun updateAttribute(baseUrl: String, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$id", method = "PUT", body = body)
    suspend fun deleteAttribute(baseUrl: String, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$id", params = params(force), method = "DELETE")
    suspend fun listAttributeTerms(baseUrl: String, attributeId: Long, page: Int = 1, perPage: Int = 100) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms", params = params(page, perPage))
    suspend fun getAttributeTerm(baseUrl: String, attributeId: Long, id: Long) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms/$id")
    suspend fun createAttributeTerm(baseUrl: String, attributeId: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms", method = "POST", body = body)
    suspend fun updateAttributeTerm(baseUrl: String, attributeId: Long, id: Long, body: String) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms/$id", method = "PUT", body = body)
    suspend fun deleteAttributeTerm(baseUrl: String, attributeId: Long, id: Long, force: Boolean = false) = request(baseUrl, "/wp-json/wc/v3/products/attributes/$attributeId/terms/$id", params = params(force), method = "DELETE")

    suspend fun listMedia(baseUrl: String, page: Int = 1, perPage: Int = 30, search: String? = null): ApiResponse = wordpressRequest(baseUrl, "/wp-json/wp/v2/media", params(page, perPage, search))

    suspend fun uploadMedia(baseUrl: String, fileName: String, bytes: ByteArray, mediaType: String): ApiResponse {
        val url = Url("${baseUrl.trimEnd('/')}/wp-json/wp/v2/media")
        require(url.protocol.name == "https") { "WordPress Media API requires HTTPS" }
        val auth = wordpressAuth() ?: return ApiResponse(401, "{\"code\":\"woogit_missing_wordpress_credentials\",\"message\":\"WordPress username and Application Password are required for media operations.\"}", "POST", url.toString())
        val safeFileName = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "image-${System.currentTimeMillis()}.jpg" }
        val response: HttpResponse = try {
            client.post(url) {
                header(HttpHeaders.Authorization, auth)
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.ContentDisposition, "attachment; filename=\"$safeFileName\"")
                contentType(ContentType.parse(mediaType.ifBlank { "application/octet-stream" }))
                setBody(bytes)
            }
        } catch (throwable: Throwable) {
            reportTechnical("Media", "WooCommerceApi.uploadMedia", "POST", url, "NetworkException", throwable = throwable)
            throw throwable
        }
        val body = response.bodyAsText()
        reportHttpFailure("Media", "WooCommerceApi.uploadMedia", "POST", url, response.status.value, body)
        return ApiResponse(response.status.value, body, "POST", url.toString())
    }

    suspend fun deleteMedia(baseUrl: String, mediaId: Long, force: Boolean = true): ApiResponse = wordpressRequest(baseUrl, "/wp-json/wp/v2/media/$mediaId", params(force), "DELETE")

    private suspend fun request(baseUrl: String, path: String, params: Map<String, Any> = emptyMap(), method: String = "GET", body: String? = null): ApiResponse {
        val url = Url("${baseUrl.trimEnd('/')}$path")
        require(url.protocol.name == "https") { "WooCommerce API requires HTTPS" }
        val response = try {
            execute(url, method, body, params)
        } catch (throwable: Throwable) {
            reportTechnical("WooCommerce", "WooCommerceApi.request", method, url, "NetworkException", throwable = throwable)
            throw throwable
        }
        val responseBody = response.bodyAsText()
        reportHttpFailure("WooCommerce", "WooCommerceApi.request", method, url, response.status.value, responseBody)
        return ApiResponse(response.status.value, responseBody, method, url.toString())
    }

    private suspend fun wordpressRequest(baseUrl: String, path: String, params: Map<String, Any> = emptyMap(), method: String = "GET"): ApiResponse {
        val url = Url("${baseUrl.trimEnd('/')}$path")
        require(url.protocol.name == "https") { "WordPress API requires HTTPS" }
        val auth = wordpressAuth() ?: return ApiResponse(401, "{\"code\":\"woogit_missing_wordpress_credentials\",\"message\":\"WordPress username and Application Password are required for media operations.\"}", method, url.toString())
        val requestUrl = URLBuilder(url).apply { params.forEach { (key, value) -> parameters.append(key, value.toString()) } }.build()
        val response: HttpResponse = try {
            when (method) {
                "DELETE" -> client.delete(requestUrl) { header(HttpHeaders.Authorization, auth); header(HttpHeaders.Accept, ContentType.Application.Json.toString()) }
                else -> client.get(requestUrl) { header(HttpHeaders.Authorization, auth); header(HttpHeaders.Accept, ContentType.Application.Json.toString()) }
            }
        } catch (throwable: Throwable) {
            reportTechnical("WordPress", "WooCommerceApi.wordpressRequest", method, url, "NetworkException", throwable = throwable)
            throw throwable
        }
        val body = response.bodyAsText()
        reportHttpFailure("WordPress", "WooCommerceApi.wordpressRequest", method, url, response.status.value, body)
        return ApiResponse(response.status.value, body, method, url.toString())
    }

    private fun reportHttpFailure(feature: String, operation: String, method: String, url: Url, status: Int, body: String) {
        if (status in 200..299) return
        reportTechnical(feature, operation, method, url, "HttpError", status.toString(), body)
    }

    private fun reportTechnical(feature: String, operation: String, method: String, url: Url, type: String, status: String = "", body: String = "", throwable: Throwable? = null) {
        technicalErrorReporter.report(TechnicalErrorContext(feature, operation, operation, type, method, url.toString().substringBefore('?'), status, body, "API request failure"), throwable)
    }

    private fun wordpressAuth(): String? {
        val username = credentials.wordpressUsername
        val password = credentials.wordpressApplicationPassword
        if (username.isNullOrBlank() || password.isNullOrBlank()) return null
        val token = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
        return "Basic $token"
    }

    private suspend fun execute(url: Url, method: String, body: String?, params: Map<String, Any>): HttpResponse {
        val requestUrl = URLBuilder(url).apply {
            params.forEach { (key, value) -> parameters.append(key, value.toString()) }
            parameters.append("consumer_key", credentials.consumerKey)
            parameters.append("consumer_secret", credentials.consumerSecret)
        }.build()
        return when (method) {
            "POST" -> client.post(requestUrl) { common(params); contentType(ContentType.Application.Json); setBody(body ?: "{}") }
            "PUT" -> client.put(requestUrl) { common(params); contentType(ContentType.Application.Json); setBody(body ?: "{}") }
            "PATCH" -> client.patch(requestUrl) { common(params); contentType(ContentType.Application.Json); setBody(body ?: "{}") }
            "DELETE" -> client.delete(requestUrl) { common(params) }
            else -> client.get(requestUrl) { common(params) }
        }
    }

    private fun requestParams(page: Int, perPage: Int, search: String? = null, status: String? = null, modifiedAfter: String? = null) = buildMap<String, Any> {
        put("page", page); put("per_page", perPage)
        if (!search.isNullOrBlank()) put("search", search)
        if (!status.isNullOrBlank()) put("status", status)
        if (!modifiedAfter.isNullOrBlank()) { put("modified_after", modifiedAfter); put("dates_are_gmt", true) }
    }
    private fun HttpRequestBuilder.common(params: Map<String, Any>) { params.forEach { (key, value) -> parameter(key, value) } }
    private fun params(page: Int, perPage: Int, search: String? = null, status: String? = null, modifiedAfter: String? = null) = requestParams(page, perPage, search, status, modifiedAfter)
    private fun params(value: Boolean) = mapOf<String, Any>("force" to value)
}

data class ApiResponse(val statusCode: Int, val body: String, val method: String = "", val endpoint: String = "")
