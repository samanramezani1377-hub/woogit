package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.runtime.Composable
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto

/**
 * Feature-facing entry point. The existing visual implementation remains
 * untouched; only its input contract is narrowed to coupon-owned state.
 */
@Composable
internal fun CouponsPage(
    state: CouponCommerceUiState,
    onEditCoupon: (Long, WooCouponCommerceWriteDto) -> Unit,
    onCreateCoupon: (WooCouponCommerceWriteDto) -> Unit,
    onDeleteCoupon: (Long) -> Unit,
) {
    CouponsPage(
        state = CommerceUiState(
            products = state.products,
            customers = state.customers,
            coupons = state.coupons,
        ),
        onEditCoupon = onEditCoupon,
        onCreateCoupon = onCreateCoupon,
        onDeleteCoupon = onDeleteCoupon,
    )
}
