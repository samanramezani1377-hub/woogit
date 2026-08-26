package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.model.CredentialPair
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.Url

class WooCommerceApi(
    private val client: HttpClient,
    private val credentials: CredentialPair,
) {
    suspend fun validateStore(baseUrl: String): ApiResponse =
        request(baseUrl, "/wp-json/wc/v3/system_status")

    suspend fun listOrders(baseUrl: String, page: Int = 1, perPage: Int = 20): ApiResponse =
        request(baseUrl, "/wp-json/wc/v3/orders", "page" to page, "per_page" to perPage)

    suspend fun getOrder(baseUrl: String, id: Long): ApiResponse =
        request(baseUrl, "/wp-json/wc/v3/orders/$id")

    suspend fun listProducts(baseUrl: String, page: Int = 1, perPage: Int = 20): ApiResponse =
        request(baseUrl, "/wp-json/wc/v3/products", "page" to page, "per_page" to perPage)

    suspend fun getProduct(baseUrl: String, id: Long): ApiResponse =
        request(baseUrl, "/wp-json/wc/v3/products/$id")

    private suspend fun request(baseUrl: String, path: String, vararg params: Pair<String, Any>): ApiResponse {
        val normalized = baseUrl.trimEnd('/')
        val url = Url("$normalized$path")
        require(url.protocol.name == "https") { "WooCommerce API requires HTTPS" }
        val response = client.get(url) {
            parameter("consumer_key", credentials.consumerKey)
            parameter("consumer_secret", credentials.consumerSecret)
            params.forEach { (key, value) -> parameter(key, value) }
        }
        return ApiResponse(response.status.value, response.bodyAsText())
    }
}

data class ApiResponse(val statusCode: Int, val body: String)
