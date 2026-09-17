package com.samanramezani1377.woogit.presentation.commerce

import com.samanramezani1377.woogit.core.domain.commerce.BarcodeLookupResult
import com.samanramezani1377.woogit.core.domain.commerce.InvoiceDocument
import com.samanramezani1377.woogit.core.domain.commerce.CouponUsageSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.CustomerSnapshot
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.BulkOrderStatusResult
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto

/**
 * Compatibility presentation model for the existing Commerce pages.
 * It is an adapter DTO only; feature state is owned by each feature ViewModel.
 */
internal data class CommerceUiState(
    val loading: Boolean = false,
    val products: List<Product> = emptyList(),
    val orders: List<Order> = emptyList(),
    val customers: List<WooCustomerCommerceDto> = emptyList(),
    val coupons: List<WooCouponCommerceDto> = emptyList(),
    val customerAggregation: List<CustomerSnapshot> = emptyList(),
    val couponAnalytics: List<CouponUsageSnapshot> = emptyList(),
    val analytics: Any? = null,
    val inventory: List<Product> = emptyList(),
    val barcodeResult: BarcodeLookupResult? = null,
    val invoice: InvoiceDocument? = null,
    val bulkOrderResults: List<BulkOrderStatusResult> = emptyList(),
    val bulkOrderTarget: com.samanramezani1377.woogit.core.domain.model.OrderStatus? = null,
    val message: String? = null,
    val error: String? = null,
    val storeId: StoreId? = null,
)