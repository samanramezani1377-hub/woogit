package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

    if (feature == CommerceFeature.CUSTOMERS || feature == CommerceFeature.COUPONS) {
        CommerceFeaturePage(
            storeId = storeId,
            dependencies = dependencies,
            feature = feature,
            onBack = onBack,
            onProduct = onOpenProduct,
        )
        return
    }

    val vm: CommerceViewModel = viewModel(
        key = "commerce-feature-${storeId.value}-${feature.name}",
        factory = CommerceViewModelFactory(dependencies, storeId),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(storeId, feature) {
        vm.load()
    }

    when (feature) {
        CommerceFeature.BARCODE -> {
            BarcodeScannerScreen(
                state = state,
                onResolve = vm::resolveBarcode,
                onProduct = onOpenProduct,
                onBack = onBack,
            )
        }
        CommerceFeature.INVENTORY,
        CommerceFeature.BULK_ORDERS,
        CommerceFeature.INVOICE -> {
            GlassScaffold {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlassTokens.spacingSm),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
                        when (feature) {
                            CommerceFeature.INVENTORY -> InventoryPage(
                                storeId = storeId,
                                state = state,
                                onFilter = vm::filterInventory,
                                onProduct = onOpenProduct,
                            )
                            CommerceFeature.BULK_ORDERS -> BulkOrdersPage(
                                state = state,
                                onBulkOrder = vm::bulkOrderStatus,
                            )
                            CommerceFeature.INVOICE -> InvoicePage(
                                state = state,
                                onInvoice = vm::prepareInvoice,
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
        CommerceFeature.ANALYTICS,
        CommerceFeature.CUSTOMERS,
        CommerceFeature.COUPONS -> Unit
    }
}
