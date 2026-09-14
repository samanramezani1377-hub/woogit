package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

/**
 * Hosts one Commerce capability as a real E11 destination.
 * The capability itself is rendered by the existing workflow UI, but navigation
 * ownership lives in E11 rather than inside CommerceCenterScreen.
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
