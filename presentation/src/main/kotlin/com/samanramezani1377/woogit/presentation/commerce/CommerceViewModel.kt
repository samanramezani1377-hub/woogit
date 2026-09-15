package com.samanramezani1377.woogit.presentation.commerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeLookupResult
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeResolver
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

internal class CommerceViewModel(private val dependencies: V1PresentationDependencies, private val storeId: StoreId) : ViewModel() {
    companion object { private const val PAGE_SIZE = 100; private const val MAX_READ_ITEMS = 10_000; private const val BATCH_SIZE = 100 }
    private val _state = MutableStateFlow(CommerceUiState())
    val state: StateFlow<CommerceUiState> = _state.asStateFlow()

    fun load(loadRemoteCommerceData: Boolean = true) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, message = null)
        try { val products = readAllProducts(); val orders = readAllOrders(); applyProductAndOrderState(products, orders); if (loadRemoteCommerceData) loadCustomersAndCoupons() }
        catch (t: Throwable) { fail("بارگذاری ابزارهای Commerce ناموفق بود: ${t.message.orEmpty()}") }
    }
    private suspend fun readAllProducts(): List<Product> { val all = mutableListOf<Product>(); var page = 1; while (all.size < MAX_READ_ITEMS) { when (val result = dependencies.getProducts(storeId, page, PAGE_SIZE, null)) { is CoreResult.Success -> { all += result.value; if (result.value.size < PAGE_SIZE) break }; is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error)) }; page++ }; return all.distinctBy { it.id.value } }
    private suspend fun readAllOrders(): List<Order> { val all = mutableListOf<Order>(); var page = 1; while (all.size < MAX_READ_ITEMS) { when (val result = dependencies.getOrders(storeId, page, PAGE_SIZE, null, null)) { is CoreResult.Success -> { all += result.value; if (result.value.size < PAGE_SIZE) break }; is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error)) }; page++ }; return all.distinctBy { it.id.value } }
    private fun applyProductAndOrderState(products: List<Product>, orders: List<Order>) { _state.value = _state.value.copy(loading = false, products = products, orders = orders, inventory = products, analytics = CommerceFeatureEngine.analytics(orders, products), customerAggregation = CommerceFeatureEngine.customersFromOrders(orders), couponAnalytics = CommerceFeatureEngine.couponsFromOrders(orders)) }
    private suspend fun loadCustomersAndCoupons() { val provider = CommerceRuntime.provider ?: return; when (val client = provider.commerceClient(storeId)) { is CoreResult.Success -> { val api = client.value.second; val customers = mutableListOf<WooCustomerCommerceDto>(); val coupons = mutableListOf<WooCouponCommerceDto>(); var page = 1; while (customers.size < MAX_READ_ITEMS) { val items = api.decodeCustomers(api.listCustomers(page, PAGE_SIZE)).getOrElse { break }; customers += items; if (items.size < PAGE_SIZE) break; page++ }; page = 1; while (coupons.size < MAX_READ_ITEMS) { val items = api.decodeCoupons(api.listCoupons(page, PAGE_SIZE)).getOrElse { break }; coupons += items; if (items.size < PAGE_SIZE) break; page++ }; _state.value = _state.value.copy(customers = customers.distinctBy { it.id }, coupons = coupons.distinctBy { it.id }) }; is CoreResult.Failure -> Unit } }

    fun resolveBarcode(value: String) { _state.value = _state.value.copy(barcodeResult = BarcodeResolver.resolve(value, _state.value.products, _state.value.orders), message = null, error = null) }
    fun filterInventory(query: String, lowStock: Boolean, outOfStock: Boolean) { _state.value = _state.value.copy(inventory = CommerceFeatureEngine.filterInventory(_state.value.products, query, lowStockOnly = lowStock, outOfStockOnly = outOfStock)) }

    fun updateCoupon(id: Long, coupon: WooCouponCommerceWriteDto) = viewModelScope.launch {
        val previous = _state.value.coupons
        val optimistic = previous.map { current -> if (current.id == id) current.copy(code = coupon.code, amount = coupon.amount, discount_type = coupon.discount_type, description = coupon.description, date_expires = coupon.date_expires, individual_use = coupon.individual_use, free_shipping = coupon.free_shipping, usage_limit = coupon.usage_limit, usage_limit_per_user = coupon.usage_limit_per_user, minimum_amount = coupon.minimum_amount, maximum_amount = coupon.maximum_amount, exclude_sale_items = coupon.exclude_sale_items) else current }
        if (optimistic == previous) return@launch
        _state.value = _state.value.copy(coupons = optimistic, loading = true, error = null, message = null)
        val client = commerceClientOrFail() ?: run { _state.value = _state.value.copy(coupons = previous, loading = false); return@launch }
        val response = client.updateCoupon(id, coupon, "commerce-coupon-$id-${System.currentTimeMillis()}")
        if (response.statusCode in 200..299) { _state.value = _state.value.copy(loading = false, message = "کوپن «${coupon.code}» به‌روزرسانی شد.", error = null); loadCustomersAndCoupons() }
        else _state.value = _state.value.copy(coupons = previous, loading = false, message = null, error = "به‌روزرسانی کوپن انجام نشد؛ تغییرات محلی به حالت قبل برگشت.")
    }

    fun createCoupon(coupon: WooCouponCommerceWriteDto) = viewModelScope.launch {
        val localId = -System.currentTimeMillis()
        val localCoupon = WooCouponCommerceDto(id = localId, code = coupon.code, amount = coupon.amount, discount_type = coupon.discount_type, description = coupon.description, date_expires = coupon.date_expires, individual_use = coupon.individual_use, free_shipping = coupon.free_shipping, usage_limit = coupon.usage_limit, usage_limit_per_user = coupon.usage_limit_per_user, minimum_amount = coupon.minimum_amount, maximum_amount = coupon.maximum_amount, exclude_sale_items = coupon.exclude_sale_items)
        val previous = _state.value.coupons
        _state.value = _state.value.copy(coupons = listOf(localCoupon) + previous, loading = true, error = null, message = "کوپن «${coupon.code}» ابتدا در لیست محلی اضافه شد؛ در حال همگام‌سازی…")
        val client = commerceClientOrFail() ?: run { _state.value = _state.value.copy(coupons = previous, loading = false, message = null, error = "ساخت کوپن در فروشگاه انجام نشد؛ تغییر محلی برگشت داده شد."); return@launch }
        val response = client.createCoupon(coupon, "commerce-coupon-create-${System.currentTimeMillis()}")
        if (response.statusCode in 200..299) { val remote = client.decodeCoupon(response).getOrNull(); _state.value = _state.value.copy(coupons = _state.value.coupons.map { if (it.id == localId) remote ?: it else it }, loading = false, message = "کوپن «${coupon.code}» ساخته و با فروشگاه همگام شد.", error = null) }
        else _state.value = _state.value.copy(coupons = previous, loading = false, message = null, error = "ساخت کوپن در فروشگاه ناموفق بود؛ تغییر محلی برگشت داده شد.")
    }

    fun prepareInvoice(orderId: String) { val order = _state.value.orders.firstOrNull { it.id.value == orderId } ?: return fail("سفارش پیدا نشد."); _state.value = _state.value.copy(invoice = InvoiceDocumentFactory.create(order, "WooGit Store")) }
    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }
    private suspend fun commerceClientOrFail(): com.samanramezani1377.woogit.data.network.WooCommerceCommerceApi? = when (val result = CommerceRuntime.provider?.commerceClient(storeId)) { is CoreResult.Success -> result.value.second; else -> { fail("اتصال Commerce برقرار نیست."); null } }
    private fun fail(message: String) { _state.value = _state.value.copy(loading = false, error = message) }
}

internal class CommerceViewModelFactory(private val dependencies: V1PresentationDependencies, private val storeId: StoreId) : androidx.lifecycle.ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = CommerceViewModel(dependencies, storeId) as T }
