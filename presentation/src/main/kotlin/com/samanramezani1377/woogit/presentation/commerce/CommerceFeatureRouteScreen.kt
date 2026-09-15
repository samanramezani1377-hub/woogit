package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.analytics.AnalyticsRouteScreen

@Composable
internal fun CommerceFeatureRouteScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    feature: CommerceFeature,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    if (feature == CommerceFeature.ANALYTICS) {
        AnalyticsRouteScreen(storeId = storeId, onBack = onBack)
        return
    }

    val vm: CommerceViewModel = viewModel(
        key = "commerce-feature-${storeId.value}-${feature.name}",
        factory = CommerceViewModelFactory(dependencies, storeId),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(storeId, feature) {
        vm.load(loadRemoteCommerceData = feature != CommerceFeature.CUSTOMERS)
    }

    if (feature == CommerceFeature.BARCODE) {
        BarcodeScannerScreen(state = state, onResolve = vm::resolveBarcode, onProduct = onOpenProduct, onBack = onBack)
    } else {
        GlassScaffold {
            Box(Modifier.fillMaxSize().padding(horizontal = GlassTokens.spacingSm)) {
                CommerceFeaturePage(
                    storeId = storeId,
                    feature = feature,
                    state = state,
                    onBack = onBack,
                    onBarcode = vm::resolveBarcode,
                    onInventoryFilter = vm::filterInventory,
                    onBulkOrder = vm::bulkOrderStatus,
                    onInvoice = vm::prepareInvoice,
                    onProduct = onOpenProduct,
                    onEditCoupon = vm::updateCoupon,
                    onCreateCoupon = vm::createCoupon,
                )
            }
        }
    }
}
