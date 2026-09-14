package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samanramezani1377.woogit.core.domain.model.OrderStatus

@Composable
internal fun CommerceFeaturePage(
    feature: CommerceFeature,
    state: CommerceUiState,
    onBack: () -> Unit,
    onBarcode: (String) -> Unit,
    onInventoryFilter: (String, Boolean, Boolean) -> Unit,
    onBulkOrder: (Set<String>, OrderStatus) -> Unit,
    onBulkCoupon: (Set<Long>, String) -> Unit,
    onInvoice: (String) -> Unit,
    onProduct: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
        when (feature) {
            CommerceFeature.BARCODE -> BarcodePage(state, onBarcode, onProduct)
            CommerceFeature.BULK_ORDERS -> BulkOrdersPage(state, onBulkOrder)
            CommerceFeature.INVENTORY -> InventoryPage(state, onInventoryFilter, onProduct)
            CommerceFeature.CUSTOMERS -> CustomersPage(state)
            CommerceFeature.ANALYTICS -> Unit
            CommerceFeature.COUPONS -> CouponsPage(state, onBulkCoupon)
            CommerceFeature.INVOICE -> InvoicePage(state, onInvoice)
        }
    }
}
