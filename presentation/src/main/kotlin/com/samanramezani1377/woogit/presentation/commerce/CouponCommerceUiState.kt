package com.samanramezani1377.woogit.presentation.commerce

import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto

/**
 * UI contract owned by the Coupons feature.
 *
 * Coupon management needs its own data plus product/customer picker data.
 * It must not consume the complete Commerce state.
 */
internal data class CouponCommerceUiState(
    val coupons: List<WooCouponCommerceDto> = emptyList(),
    val products: List<Product> = emptyList(),
    val customers: List<WooCustomerCommerceDto> = emptyList(),
)
