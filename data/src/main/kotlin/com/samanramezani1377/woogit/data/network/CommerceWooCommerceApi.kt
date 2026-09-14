package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.CredentialPair
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val commerceJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
data class WooCustomerCommerceDto(
    val id: Long,
    val email: String? = null,
    val username: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val role: String? = null,
    val billing: WooAddressDto? = null,
    val shipping: WooAddressDto? = null,
    val is_paying_customer: Boolean = false,
    val avatar_url: String? = null,
    val date_created: String? = null,
    val date_modified: String? = null,
    val orders_count: Int = 0,
    val total_spent: String = "0",
)

@Serializable
data class WooCouponCommerceDto(
    val id: Long,
    val code: String = "",
    val amount: String = "0",
    val discount_type: String = "percent",
    val description: String? = null,
    val date_expires: String? = null,
    val individual_use: Boolean = false,
    val free_shipping: Boolean = false,
    val usage_count: Int = 0,
    val usage_limit: Int? = null,
    val usage_limit_per_user: Int? = null,
    val minimum_amount: String = "",
    val maximum_amount: String = "",
    val exclude_sale_items: Boolean = false,
)

data class WooCommerceCommerceApi(
    private val backend: BackendClient,
    private val storeId: String,
    private val credentials: CredentialPair,
) {
    suspend fun listCustomers(
        page: Int = 1,
        perPage: Int = 20,
        search: String? = null,
    ): ApiResponse = get(
        "/wp-json/wc/v3/customers",
        pageQuery(page, perPage, search),
    )

    suspend fun getCustomer(id: Long): ApiResponse = get(
        "/wp-json/wc/v3/customers/$id",
    )

    suspend fun createCustomer(
        customer: WooCustomerCommerceDto,
        idempotencyKey: String? = null,
    ): ApiResponse = mutate(
        "/wp-json/wc/v3/customers",
        "POST",
        commerceJson.encodeToString(customer),
        idempotencyKey,
    )

    suspend fun updateCustomer(
        id: Long,
        customer: WooCustomerCommerceDto,
        idempotencyKey: String? = null,
    ): ApiResponse = mutate(
        "/wp-json/wc/v3/customers/$id",
        "PUT",
        commerceJson.encodeToString(customer),
        idempotencyKey,
    )

    suspend fun deleteCustomer(
        id: Long,
        force: Boolean = true,
        idempotencyKey: String? = null,
    ): ApiResponse = mutate(
        "/wp-json/wc/v3/customers/$id",
        "DELETE",
        null,
        idempotencyKey,
        mapOf("force" to force),
    )

    suspend fun listCoupons(
        page: Int = 1,
        perPage: Int = 20,
        search: String? = null,
    ): ApiResponse = get(
        "/wp-json/wc/v3/coupons",
        pageQuery(page, perPage, search),
    )

    suspend fun getCoupon(id: Long): ApiResponse = get(
        "/wp-json/wc/v3/coupons/$id",
    )

    suspend fun createCoupon(
        coupon: WooCouponCommerceDto,
        idempotencyKey: String? = null,
    ): ApiResponse = mutate(
        "/wp-json/wc/v3/coupons",
        "POST",
        commerceJson.encodeToString(coupon),
        idempotencyKey,
    )

    suspend fun updateCoupon(
        id: Long,
        coupon: WooCouponCommerceDto,
        idempotencyKey: String? = null,
    ): ApiResponse = mutate(
        "/wp-json/wc/v3/coupons/$id",
        "PUT",
        commerceJson.encodeToString(coupon),
        idempotencyKey,
    )

    suspend fun deleteCoupon(
        id: Long,
        force: Boolean = true,
        idempotencyKey: String? = null,
    ): ApiResponse = mutate(
        "/wp-json/wc/v3/coupons/$id",
        "DELETE",
        null,
        idempotencyKey,
        mapOf("force" to force),
    )

    suspend fun batchUpdateOrderStatuses(
        updates: List<Pair<Long, String>>,
        idempotencyKey: String? = null,
    ): ApiResponse {
        val payload = buildString {
            append("{\"update\":[")
            updates.forEachIndexed { index, update ->
                if (index > 0) {
                    append(',')
                }
                append("{\"id\":")
                append(update.first)
                append(",\"status\":\"")
                append(update.second.replace("\\", "\\\\").replace("\"", "\\\""))
                append("\"}")
            }
            append("]}")
        }
        return mutate(
            "/wp-json/wc/v3/orders/batch",
            "POST",
            payload,
            idempotencyKey,
        )
    }

    suspend fun decodeCustomers(response: ApiResponse): Result<List<WooCustomerCommerceDto>> =
        decode(response)

    suspend fun decodeCustomer(response: ApiResponse): Result<WooCustomerCommerceDto> =
        decode(response)

    suspend fun decodeCoupons(response: ApiResponse): Result<List<WooCouponCommerceDto>> =
        decode(response)

    suspend fun decodeCoupon(response: ApiResponse): Result<WooCouponCommerceDto> =
        decode(response)

    private suspend fun get(
        path: String,
        query: Map<String, Any> = emptyMap(),
    ): ApiResponse = backend.forward(
        storeId,
        path,
        "GET",
        credentials,
        query,
    )

    private suspend fun mutate(
        path: String,
        method: String,
        body: String?,
        idempotencyKey: String? = null,
        query: Map<String, Any> = emptyMap(),
    ): ApiResponse = backend.forward(
        storeId,
        path,
        method,
        credentials,
        query,
        body,
        idempotencyKey,
    )

    private fun pageQuery(
        page: Int,
        perPage: Int,
        search: String?,
    ): Map<String, Any> = buildMap {
        put("page", page)
        put("per_page", perPage)
        if (!search.isNullOrBlank()) {
            put("search", search)
        }
    }

    private inline fun <reified T> decode(response: ApiResponse): Result<T> =
        if (response.statusCode in 200..299) {
            runCatching {
                commerceJson.decodeFromString<T>(response.body)
            }
        } else {
            Result.failure(HttpApiException(response.statusCode, response.body))
        }
}
