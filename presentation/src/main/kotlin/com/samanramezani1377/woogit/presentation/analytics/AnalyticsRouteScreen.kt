package com.samanramezani1377.woogit.presentation.analytics

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
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.commerce.AnalyticsPage
import com.samanramezani1377.woogit.presentation.commerce.FeatureHeader

@Composable
internal fun AnalyticsRouteScreen(
    storeId: StoreId,
    onBack: () -> Unit,
) {
    val vm: AnalyticsViewModel = viewModel(
        key = "analytics-local-${storeId.value}",
        factory = AnalyticsViewModelFactory(storeId),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(storeId) {
        vm.load(AnalyticsRange.YEAR)
    }

    GlassScaffold {
        Box(Modifier.fillMaxSize().padding(horizontal = GlassTokens.spacingSm)) {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                FeatureHeader("تحلیل فروش", "تحلیل سفارش‌ها و محصولات ذخیره‌شده در دستگاه", onBack)
                AnalyticsPage(
                    analytics = state.analytics,
                    loading = state.loading,
                    selectedRange = state.range,
                    error = state.error,
                    onRangeSelected = vm::load,
                )
            }
        }
    }
}
