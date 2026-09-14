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
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

/**
 * Hosts one Commerce capability as a real E11 destination.
 *
 * The feature keeps its own workflow/content, but its visual shell is the same
 * WooGit presentation shell used by the rest of the app. A capability route
 * must never become a second UI system just because it is navigated separately.
 */
@Composable
internal fun CommerceFeatureRouteScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    feature: CommerceFeature,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val vm: CommerceViewModel = viewModel(
        key = "commerce-feature-${storeId.value}-${feature.name}",
        factory = CommerceViewModelFactory(dependencies, storeId),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(storeId) { vm.load() }

    GlassScaffold {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = com.samanramezani1377.woogit.presentation.GlassTokens.spacingSm),
        ) {
            CommerceFeaturePage(
                feature = feature,
                state = state,
                onBack = onBack,
                onBarcode = vm::resolveBarcode,
                onInventoryFilter = vm::filterInventory,
                onBulkOrder = vm::bulkOrderStatus,
                onBulkCustomer = vm::bulkCustomerRole,
                onBulkCoupon = vm::bulkCouponAmount,
                onInvoice = vm::prepareInvoice,
                onProduct = onOpenProduct,
                onOrder = onOpenOrder,
            )
        }
    }
}
