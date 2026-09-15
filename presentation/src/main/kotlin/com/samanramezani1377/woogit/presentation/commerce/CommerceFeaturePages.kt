package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto

@Composable
internal fun CommerceFeaturePage(
    storeId: StoreId,
    feature: CommerceFeature,
    state: CommerceUiState,
    onBack: () -> Unit,
    onBarcode: (String) -> Unit,
    onInventoryFilter: (String, Boolean, Boolean) -> Unit,
    onBulkOrder: (Set<String>, OrderStatus) -> Unit,
    onInvoice: (String) -> Unit,
    onProduct: (String) -> Unit,
    onEditCoupon: (Long, WooCouponCommerceWriteDto) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
        when (feature) {
            CommerceFeature.BARCODE -> BarcodePage(state, onBarcode, onProduct)
            CommerceFeature.BULK_ORDERS -> BulkOrdersPage(state, onBulkOrder)
            CommerceFeature.INVENTORY -> InventoryPage(storeId, state, onInventoryFilter, onProduct)
            CommerceFeature.CUSTOMERS -> CustomersPage(storeId = storeId, state = state)
            CommerceFeature.ANALYTICS -> Unit
            CommerceFeature.COUPONS -> CouponsPage(state, onEditCoupon)
            CommerceFeature.INVOICE -> InvoicePage(state, onInvoice)
        }
    }
}
