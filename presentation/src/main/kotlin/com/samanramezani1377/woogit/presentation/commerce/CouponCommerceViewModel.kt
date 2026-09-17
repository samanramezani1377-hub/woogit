package com.samanramezani1377.woogit.presentation.commerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CouponCommerceViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_READ_ITEMS = 10_000
        private const val REFRESH_MS = 60_000L
    }

    private val _state = MutableStateFlow(CouponCommerceUiState())
    val state: StateFlow<CouponCommerceUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_MS)
                refresh()
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        loadReferenceData()
        loadCoupons()
    }

    fun updateCoupon(id: Long, coupon: WooCouponCommerceWriteDto) = viewModelScope.launch {
        val previous = _state.value.coupons
        val optimistic = previous.map { current ->
            if (current.id == id) current.copy(
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
            ) else current
        }
        if (optimistic == previous) return@launch
        _state.value = _state.value.copy(coupons = optimistic)
        val client = commerceClient() ?: run { _state.value = _state.value.copy(coupons = previous); return@launch }
        val response = client.updateCoupon(id, coupon, "commerce-coupon-$id-${System.currentTimeMillis()}")
        if (response.statusCode !in 200..299) _state.value = _state.value.copy(coupons = previous)
        else loadCoupons()
    }

    fun createCoupon(coupon: WooCouponCommerceWriteDto) = viewModelScope.launch {
        val localId = -System.currentTimeMillis()
        val localCoupon = WooCouponCommerceDto(
            id = localId,
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
        val previous = _state.value.coupons
        _state.value = _state.value.copy(coupons = listOf(localCoupon) + previous)
        val client = commerceClient() ?: run { _state.value = _state.value.copy(coupons = previous); return@launch }
        val response = client.createCoupon(coupon, "commerce-coupon-create-${System.currentTimeMillis()}")
        if (response.statusCode in 200..299) loadCoupons()
        else _state.value = _state.value.copy(coupons = previous)
    }

    fun deleteCoupon(id: Long) = viewModelScope.launch {
        val previous = _state.value.coupons
        if (previous.none { it.id == id }) return@launch
        _state.value = _state.value.copy(coupons = previous.filterNot { it.id == id })
        val client = commerceClient() ?: run { _state.value = _state.value.copy(coupons = previous); return@launch }
        val response = client.deleteCoupon(id, force = true, idempotencyKey = "commerce-coupon-delete-$id-${System.currentTimeMillis()}")
        if (response.statusCode !in 200..299) _state.value = _state.value.copy(coupons = previous)
    }

    private suspend fun loadReferenceData() {
        val products = mutableListOf<com.samanramezani1377.woogit.core.domain.model.Product>()
        var page = 1
        while (products.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getProducts(storeId, page, PAGE_SIZE, null)) {
                is CoreResult.Success -> {
                    products += result.value
                    if (result.value.size < PAGE_SIZE) break
                }
                is CoreResult.Failure -> break
            }
            page++
        }

        val provider = CommerceRuntime.provider ?: return
        when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val api = client.value.second
                val customers = mutableListOf<WooCustomerCommerceDto>()
                page = 1
                while (customers.size < MAX_READ_ITEMS) {
                    val pageItems = api.decodeCustomers(api.listCustomers(page, PAGE_SIZE)).getOrElse { break }
                    customers += pageItems
                    if (pageItems.size < PAGE_SIZE) break
                    page++
                }
                _state.value = _state.value.copy(
                    products = products.distinctBy { it.id.value },
                    customers = customers.distinctBy { it.id },
                )
            }
            is CoreResult.Failure -> _state.value = _state.value.copy(products = products.distinctBy { it.id.value })
        }
    }

    private suspend fun loadCoupons() {
        val provider = CommerceRuntime.provider ?: return
        when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val api = client.value.second
                val coupons = mutableListOf<WooCouponCommerceDto>()
                var page = 1
                while (coupons.size < MAX_READ_ITEMS) {
                    val pageItems = api.decodeCoupons(api.listCoupons(page, PAGE_SIZE)).getOrElse { break }
                    coupons += pageItems
                    if (pageItems.size < PAGE_SIZE) break
                    page++
                }
                _state.value = _state.value.copy(coupons = coupons.distinctBy { it.id })
            }
            is CoreResult.Failure -> Unit
        }
    }

    private suspend fun commerceClient(): com.samanramezani1377.woogit.data.network.WooCommerceCommerceApi? =
        when (val result = CommerceRuntime.provider?.commerceClient(storeId)) {
            is CoreResult.Success -> result.value.second
            is CoreResult.Failure, null -> null
        }
}

internal class CouponCommerceViewModelFactory(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CouponCommerceViewModel(dependencies, storeId) as T
}
