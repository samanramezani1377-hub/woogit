package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

@Composable
internal fun CommerceFeaturePage(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    feature: CommerceFeature,
    onBack: () -> Unit,
    onProduct: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
        when (feature) {
            CommerceFeature.CUSTOMERS -> {
                val vm: CustomerCommerceViewModel = viewModel(
                    key = "commerce-customers-${storeId.value}",
                    factory = CustomerCommerceViewModelFactory(dependencies, storeId),
                )
                val state by vm.state.collectAsStateWithLifecycle()
                CustomersPage(storeId = storeId, state = state)
            }
            CommerceFeature.COUPONS -> {
                val vm: CouponCommerceViewModel = viewModel(
                    key = "commerce-coupons-${storeId.value}",
                    factory = CouponCommerceViewModelFactory(dependencies, storeId),
                )
                val state by vm.state.collectAsStateWithLifecycle()
                CouponsPage(
                    state = state,
                    onEditCoupon = vm::updateCoupon,
                    onCreateCoupon = vm::createCoupon,
                    onDeleteCoupon = vm::deleteCoupon,
                )
            }
            else -> Unit
        }
    }
}
