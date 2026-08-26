package com.samanramezani1377.woogit.data.remote

import com.samanramezani1377.woogit.data.network.ApiResponse
import com.samanramezani1377.woogit.data.network.WooCommerceApi

data class RemotePage(val response: ApiResponse, val page: Int, val perPage: Int)

class OrderRemoteDataSource(private val api: WooCommerceApi) {
    suspend fun list(baseUrl: String, page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null) =
        api.listOrders(baseUrl, page, perPage, search, status)
    suspend fun get(baseUrl: String, id: Long) = api.getOrder(baseUrl, id)
    suspend fun update(baseUrl: String, id: Long, body: String) = api.updateOrder(baseUrl, id, body)
}

class ProductRemoteDataSource(private val api: WooCommerceApi) {
    suspend fun list(baseUrl: String, page: Int = 1, perPage: Int = 20, search: String? = null) =
        api.listProducts(baseUrl, page, perPage, search)
    suspend fun get(baseUrl: String, id: Long) = api.getProduct(baseUrl, id)
    suspend fun create(baseUrl: String, body: String) = api.createProduct(baseUrl, body)
    suspend fun update(baseUrl: String, id: Long, body: String) = api.updateProduct(baseUrl, id, body)
    suspend fun delete(baseUrl: String, id: Long, force: Boolean = false) = api.deleteProduct(baseUrl, id, force)
}

class StoreRemoteDataSource(private val api: WooCommerceApi) {
    suspend fun validate(baseUrl: String) = api.validateStore(baseUrl)
}
