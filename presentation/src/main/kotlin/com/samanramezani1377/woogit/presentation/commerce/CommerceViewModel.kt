package com.samanramezani1377.woogit.presentation.commerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeLookupResult
import com.samanramezani1377-hub.woogit.core.domain.commerce.BarcodeResolver
import com.samanramezani1377.woogit.core.domain.commerce.CommerceFeatureEngine
import com.samanramezani1377.woogit.core.domain.commerce.CouponUsageSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.CustomerSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.InvoiceDocumentFactory
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.data.network.BulkOrderStatusMapper
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceWriteDto
import com.samanramezani1377.woogit.presentation.PresentationErrorMapper
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class CommerceUiState(
    val loading: Boolean = false,
    val products: List<Product> = emptyList(),
    val orders: List<Order> = emptyList(),
    val customers: List<WooCustomerCommerceDto> = emptyList(),
    val coupons: List<WooCouponCommerceDto> = emptyList(),
    val customerAggregation: List<CustomerSnapshot> = emptyList(),
    val couponAnalytics: List<CouponUsageSnapshot> = emptyList(),
    val analytics: AnalyticsSnapshot? = null,
    val inventory: List<Product> = emptyList(),
    val barcodeResult: BarcodeLookupResult? = null,
    val invoice: InvoiceDocument? = null,
    val bulkOrderResults: List<BulkOrderStatusResult> = emptyList(),
    val bulkOrderTarget: OrderStatus? = null,
    val message: String? = null,
    val error: String? = null,
)

internal class CommerceViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_READ_ITEMS = 10_000
        private const val BATCH_SIZE = 100
    }

    private val _state = MutableStateFlow(CommerceUiState())
    val state: StateFlow<CommerceUiState> = _state.asStateFlow()

    fun load(loadRemoteCommerceData: Boolean = true) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, message = null)
        try {
            val products = readAllProducts()
            val orders = readAllOrders()
            applyProductAndOrderState(products, orders)
            if (loadRemoteCommerceData) loadCustomersAndCoupons()
        } catch (t: Throwable) {
            fail("بارگذاری ابزارهای Commerce ناموفق بود: ${t.message.orEmpty()}")
        }
    }

    private suspend fun readAllProducts(): List<Product> {
        val all = mutableListOf<Product>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getProducts(storeId, page, PAGE_SIZE, null)) {
                is CoreResult.Success -> {
                    all += result.value
                    if (result.value.size < PAGE_SIZE) break
                }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.id.value }
    }

    private suspend fun readAllOrders(): List<Order> {
        val all = mutableListOf<Order>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getOrders(storeId, page, PAGE_SIZE, null, null)) {
                is CoreResult.Success -> {
                    all += result.value
                    if (result.value.size < PAGE_SIZE) break
                }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.id.value }
    }

    private fun applyProductAndOrderState(products: List<Product>, orders: List<Order>) {
        _state.value = _state.value.copy(
            loading = false,
            products = products,
            orders = orders,
            inventory = products,
            analytics = CommerceFeatureEngine.analytics(orders, products),
            customerAggregation = CommerceFeatureEngine.customersFromOrders(orders),
            couponAnalytics = CommerceFeatureEngine.couponsFromOrders(orders),
        )
    }

    private suspend fun refreshOrders() {
        val orders = readAllOrders()
        applyProductAndOrderState(_state.value.products, orders)
    }

    private suspend fun loadCustomersAndCoupons() {
        val provider = CommerceRuntime.provider ?: return
        when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val api = client.value.second
                val customers = mutableListOf<WooCustomerCommerceDto>()
                val coupons = mutableListOf<WooCouponCommerceDto>()
                var page = 1
                while (customers.size < MAX_READ_ITEMS) {
                    val pageItems = api.decodeCustomers(api.listCustomers(page, PAGE_SIZE)).getOrElse { break }
                    customers += pageItems
                    if (pageItems.size < PAGE_SIZE) break
                    page++
                }
                page = 1
                while (coupons.size < MAX_READ_ITEMS) {
                    val pageItems = api.decodeCoupons(api.listCoupons(page, PAGE_SIZE)).getOrElse { break }
                    coupons += pageItems
                    if (pageItems.size < PAGE_SIZE) break
                    page++
                }
                _state.value = _state.value.copy(
                    customers = customers.distinctBy { it.id },
                    coupons = coupons.distinctBy { it.id },
                )
            }
            is CoreResult.Failure -> Unit
        }
    }

    fun resolveBarcode(value: String) {
        _state.value = _state.value.copy(
            barcodeResult = BarcodeResolver.resolve(value, _state.value.products, _state.value.orders),
            message = null,
            error = null,
        )
    }

    fun filterInventory(query: String, lowStock: Boolean, outOfStock: Boolean) {
        _state.value = _state.value.copy(
            inventory = CommerceFeatureEngine.filterInventory(
                _state.value.products,
                query,
                lowStockOnly = lowStock,
                outOfStockOnly = outOfStock,
            ),
        )
    }

    fun bulkOrderStatus(selectedIds: Set<String>, target: OrderStatus) = viewModelScope.launch {
        if (selectedIds.isEmpty() || target == OrderStatus.OTHER) return@launch
        val plan = CommerceFeatureEngine.planBulkOrderStatusUpdate(_state.value.orders, selectedIds, target)
        if (plan.isEmpty()) {
            _state.value = _state.value.copy(
                bulkOrderResults = emptyList(),
                bulkOrderTarget = target,
                message = "همه سفارش‌های انتخاب‌شده از قبل در وضعیت «${target.faLabel()}» هستند.",
                error = null,
            )
            return@launch
        }
        val client = commerceClientOrFail() ?: return@launch
        _state.value = _state.value.copy(loading = true, error = null, message = null, bulkOrderTarget = target)

        val operationId = "commerce-orders-${System.currentTimeMillis()}"
        val results = mutableListOf<BulkOrderStatusResult>()
        plan.chunked(BATCH_SIZE).forEachIndexed { chunkIndex, chunk ->
            val numericIds = mutableListOf<Long>()
            chunk.forEach { item ->
                val numericId = item.orderId.toLongOrNull()
                if (numericId == null) {
                    results += BulkOrderStatusResult(
                        orderId = EntityId(item.orderId),
                        succeeded = false,
                        error = "شناسه سفارش نامعتبر است.",
                    )
                } else {
                    numericIds += numericId
                }
            }
            if (numericIds.isEmpty()) return@forEachIndexed
            val response = client.batchUpdateOrderStatuses(
                numericIds.map { it to target.name.lowercase() },
                "$operationId-$chunkIndex",
            )
            results += BulkOrderStatusMapper.map(response, numericIds)
        }

        val succeeded = results.count { it.succeeded }
        val failed = results.count { !it.succeeded }
        val succeededIds = results.asSequence().filter { it.succeeded }.map { it.orderId.value }.toSet()
        val updatedOrders = if (succeededIds.isEmpty()) {
            _state.value.orders
        } else {
            _state.value.orders.map { order -> if (order.id.value in succeededIds) order.copy(status = target) else order }
        }

        _state.value = _state.value.copy(
            loading = false,
            orders = updatedOrders,
            analytics = CommerceFeatureEngine.analytics(updatedOrders, _state.value.products),
            customerAggregation = CommerceFeatureEngine.customersFromOrders(updatedOrders),
            couponAnalytics = CommerceFeatureEngine.couponsFromOrders(updatedOrders),
            bulkOrderResults = results,
            bulkOrderTarget = target,
            message = "وضعیت $succeeded سفارش به‌روزرسانی شد${if (failed > 0) "؛ $failed مورد ناموفق" else ""}.",
            error = if (failed > 0) "بخشی از عملیات ناموفق بود. سفارش‌های ناموفق را بررسی یا دوباره تلاش کنید." else null,
        )
    }

    fun retryFailedBulkOrderStatus() {
        val target = _state.value.bulkOrderTarget ?: return
        val failedIds = _state.value.bulkOrderResults.filterNot { it.succeeded }.map { it.orderId.value }.toSet()
        if (failedIds.isNotEmpty()) bulkOrderStatus(failedIds, target)
    }

    fun bulkCustomerRole(selectedIds: Set<Long>, role: String) = viewModelScope.launch {
        if (selectedIds.isEmpty()) return@launch
        val client = commerceClientOrFail() ?: return@launch
        val updates = _state.value.customers.filter { it.id in selectedIds }.map {
            it.id to WooCustomerCommerceWriteDto(
                email = it.email,
                username = it.username,
                first_name = it.first_name,
                last_name = it.last_name,
                role = role,
                billing = it.billing,
                shipping = it.shipping,
            )
        }
        if (updates.isEmpty()) return@launch
        var succeeded = 0
        var failed = 0
        updates.chunked(BATCH_SIZE).forEach { chunk ->
            val response = client.batchUpdateCustomers(chunk, "commerce-customers-${System.currentTimeMillis()}")
            if (response.statusCode in 200..299) succeeded += chunk.size else failed += chunk.size
        }
        _state.value = _state.value.copy(
            message = "$succeeded مشتری به‌روزرسانی شد${if (failed > 0) "؛ $failed مورد ناموفق" else ""}.",
            error = if (failed > 0) "بخشی از عملیات مشتریان ناموفق بود." else null,
        )
        loadCustomersAndCoupons()
    }

    fun updateCoupon(id: Long, coupon: WooCouponCommerceWriteDto) = viewModelScope.launch {
        val previous = _state.value.coupons
        val optimistic = previous.map { current ->
            if (current.id != id) current else current.copy(
                code = coupon.code,
                amount = coupon.amount,
                discount_type = coupon.discount_type,
                description = coupon.description,
                date_expires = coupon.date_expires,
                individual_use = coupon.individual_use,
                free_shipping = coupon.free_shipping,
                usage_limit = coupon.usage_limit,
                usage_limit_per_user = coupon.usage_limit_per_user,
                minimum_amount = coupon.minimum_amount,
                maximum_amount = coupon.maximum_amount,
                exclude_sale_items = coupon.exclude_sale_items,
            )
        }
        if (optimistic == previous) return@launch

        // Local-first: render the complete edit immediately, then reconcile with WooCommerce.
        _state.value = _state.value.copy(coupons = optimistic, loading = true, error = null, message = null)
        val client = commerceClientOrFail()
        if (client == null) {
            _state.value = _state.value.copy(coupons = previous, loading = false)
            return@launch
        }

        val response = client.updateCoupon(id, coupon, "commerce-coupon-$id-${System.currentTimeMillis()}")
        if (response.statusCode in 200..299) {
            _state.value = _state.value.copy(
                loading = false,
                message = "کوپن «${coupon.code}» به‌روزرسانی شد.",
                error = null,
            )
            loadCustomersAndCoupons()
        } else {
            _state.value = _state.value.copy(
                coupons = previous,
                loading = false,
                message = null,
                error = "به‌روزرسانی کوپن انجام نشد؛ تغییرات محلی به حالت قبل برگشت.",
            )
        }
    }

    fun bulkCouponAmount(selectedIds: Set<Long>, amount: String) = viewModelScope.launch {
        if (selectedIds.isEmpty()) return@launch
        val client = commerceClientOrFail() ?: return@launch
        val updates = _state.value.coupons.filter { it.id in selectedIds }.map {
            it.id to WooCouponCommerceWriteDto(
                code = it.code,
                amount = amount,
                discount_type = it.discount_type,
                description = it.description,
                date_expires = it.date_expires,
                individual_use = it.individual_use,
                free_shipping = it.free_shipping,
                usage_limit = it.usage_limit,
                usage_limit_per_user = it.usage_limit_per_user,
                minimum_amount = it.minimum_amount,
                maximum_amount = it.maximum_amount,
                exclude_sale_items = it.exclude_sale_items,
            )
        }
        if (updates.isEmpty()) return@launch
        var succeeded = 0
        var failed = 0
        updates.chunked(BATCH_SIZE).forEach { chunk ->
            val response = client.batchUpdateCoupons(chunk, "commerce-coupons-${System.currentTimeMillis()}")
            if (response.statusCode in 200..299) succeeded += chunk.size else failed += chunk.size
        }
        _state.value = _state.value.copy(
            message = "$succeeded کوپن به‌روزرسانی شد${if (failed > 0) "؛ $failed مورد ناموفق" else ""}.",
            error = if (failed > 0) "بخشی از عملیات کوپن‌ها ناموفق بود." else null,
        )
        loadCustomersAndCoupons()
    }

    fun prepareInvoice(orderId: String) {
        val order = _state.value.orders.firstOrNull { it.id.value == orderId } ?: return fail("سفارش پیدا نشد.")
        _state.value = _state.value.copy(invoice = InvoiceDocumentFactory.create(order, "WooGit Store"))
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }

    private suspend fun commerceClientOrFail(): com.samanramezani1377.woogit.data.network.WooCommerceCommerceApi? {
        return when (val result = CommerceRuntime.provider?.commerceClient(storeId)) {
            is CoreResult.Success -> result.value.second
            else -> { fail("اتصال Commerce برقرار نیست."); null }
        }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(loading = false, error = message)
    }
}

internal class CommerceViewModelFactory(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CommerceViewModel(dependencies, storeId) as T
}
