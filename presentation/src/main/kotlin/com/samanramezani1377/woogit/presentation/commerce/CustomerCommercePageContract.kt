package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.runtime.Composable
import com.samanramezani1377.woogit.core.domain.entity.StoreId

/**
 * Feature-facing entry point. The existing visual implementation remains
 * untouched; only its input contract is narrowed to customer-owned state.
 */
@Composable
internal fun CustomersPage(storeId: StoreId, state: CustomerCommerceUiState) {
    CustomersPage(
        storeId = storeId,
        state = CommerceUiState(orders = state.orders),
    )
}
