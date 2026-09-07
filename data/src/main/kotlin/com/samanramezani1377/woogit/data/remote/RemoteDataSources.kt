package com.samanramezani1377.woogit.data.remote

import com.samanramezani1377.woogit.data.network.ApiResponse
import com.samanramezani1377.woogit.data.network.WooCommerceApi

data class RemotePage(val response: ApiResponse, val page: Int, val perPage: Int)

class OrderRemoteDataSource(private val api: WooCommerceApi) {
    suspend fun list(page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null) =
        api.listOrders(page, perPage, search, status)
    suspend fun get(id: Long) = api.getOrder(id)
    suspend fun update(id: Long, body: String) = api.updateOrder(id, body)
}

class ProductRemoteDataSource(private val api: WooCommerceApi) {
    suspend fun list(page: Int = 1, perPage: Int = 20, search: String? = null) =
        api.listProducts(page, perPage, search)
    suspend fun get(id: Long) = api.getProduct(id)
    suspend fun create(body: String) = api.createProduct(body)
    suspend fun update(id: Long, body: String) = api.updateProduct(id, body)
    suspend fun delete(id: Long, force: Boolean = false) = api.deleteProduct(id, force)
}

class StoreRemoteDataSource(private val api: WooCommerceApi) {
    suspend fun validate() = api.validateStore()
}
