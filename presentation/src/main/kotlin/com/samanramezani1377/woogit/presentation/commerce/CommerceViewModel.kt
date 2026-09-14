package com.samanramezani1377.woogit.presentation.commerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeLookupResult
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeResolver
import com.samanramezani1377.woogit.core.domain.commerce.CommerceFeatureEngine
import com.samanramezani1377.woogit.core.domain.commerce.CouponUsageSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.CustomerSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.InvoiceDocumentFactory
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceWriteDto
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
    val message: String? = null,
    val error: String? = null,
)

internal class CommerceViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _state = MutableStateFlow(CommerceUiState())
    val state: StateFlow<CommerceUiState> = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val products = when (val result = dependencies.getProducts(storeId, 1, 100, null)) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> return@launch fail(PresentationErrorMapper.message(result.error))
            }
            val orders = when (val result = dependencies.getOrders(storeId, 1, 100, null, null)) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> return@launch fail(PresentationErrorMapper.message(result.error))
            }
            val analytics = CommerceFeatureEngine.analytics(orders, products)
            _state.value = _state.value.copy(
                loading = false,
                products = products,
                orders = orders,
                inventory = products,
                analytics = analytics,
                customerAggregation = CommerceFeatureEngine.customersFromOrders(orders),
                couponAnalytics = CommerceFeatureEngine.couponsFromOrders(orders),
            )
            loadCustomersAndCoupons()
        } catch (t: Throwable) {
            fail("بارگذاری ابزارهای Commerce ناموفق بود: ${t.message.orEmpty()}")
        }
    }

    private suspend fun loadCustomersAndCoupons() {
        val provider = CommerceRuntime.provider ?: return
        when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val api = client.value.second
                val customers = api.decodeCustomers(api.listCustomers(1, 100)).getOrElse { emptyList() }
                val coupons = api.decodeCoupons(api.listCoupons(1, 100)).getOrElse { emptyList() }
                _state.value = _state.value.copy(customers = customers, coupons = coupons)
            }
            is CoreResult.Failure -> Unit
        }
    }

    fun resolveBarcode(value: String) {
        _state.value = _state.value.copy(
            barcodeResult = BarcodeResolver.resolve(value, _state.value.products, _state.value.orders),
            message = null,
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
        if (selectedIds.isEmpty()) return@launch
        val plan = CommerceFeatureEngine.planBulkOrderStatusUpdate(_state.value.orders, selectedIds, target)
        if (plan.isEmpty()) return@launch
        val client = CommerceRuntime.provider?.commerceClient(storeId)
        if (client !is CoreResult.Success) return@launch fail("اتصال Commerce برقرار نیست.")
        val response = client.value.second.batchUpdateOrderStatuses(
            plan.map { it.orderId.toLongOrNull() to it.targetStatus.name.lowercase() }
                .mapNotNull { (id, status) -> id?.let { it to status } },
            "commerce-orders-${System.currentTimeMillis()}",
        )
        if (response.statusCode !in 200..299) fail("تغییر گروهی وضعیت سفارش‌ها ناموفق بود.")
        else _state.value = _state.value.copy(message = "وضعیت ${plan.size} سفارش به‌روزرسانی شد.")
        load()
    }

    fun bulkCustomerRole(selectedIds: Set<Long>, role: String) = viewModelScope.launch {
        val client = CommerceRuntime.provider?.commerceClient(storeId)
        if (client !is CoreResult.Success) return@launch fail("اتصال Commerce برقرار نیست.")
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
        val response = client.value.second.batchUpdateCustomers(updates, "commerce-customers-${System.currentTimeMillis()}")
        if (response.statusCode !in 200..299) fail("عملیات گروهی مشتریان ناموفق بود.")
        else _state.value = _state.value.copy(message = "${updates.size} مشتری به‌روزرسانی شد.")
        loadCustomersAndCoupons()
    }

    fun bulkCouponAmount(selectedIds: Set<Long>, amount: String) = viewModelScope.launch {
        val client = CommerceRuntime.provider?.commerceClient(storeId)
        if (client !is CoreResult.Success) return@launch fail("اتصال Commerce برقرار نیست.")
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
        val response = client.value.second.batchUpdateCoupons(updates, "commerce-coupons-${System.currentTimeMillis()}")
        if (response.statusCode !in 200..299) fail("عملیات گروهی کوپن‌ها ناموفق بود.")
        else _state.value = _state.value.copy(message = "${updates.size} کوپن به‌روزرسانی شد.")
        loadCustomersAndCoupons()
    }

    fun prepareInvoice(orderId: String) {
        val order = _state.value.orders.firstOrNull { it.id.value == orderId } ?: return fail("سفارش پیدا نشد.")
        _state.value = _state.value.copy(invoice = InvoiceDocumentFactory.create(order, "WooGit Store"))
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }

    private fun fail(message: String) {
        _state.value = _state.value.copy(loading = false, error = message)
    }
}

internal class CommerceViewModelFactory(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CommerceViewModel(dependencies, storeId) as T
}
