package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassLoading

@Composable
internal fun AnalyticsPage(
    analytics: AnalyticsSnapshot?,
    loading: Boolean,
    selectedRange: AnalyticsRange,
    error: String?,
    onRangeSelected: (AnalyticsRange) -> Unit,
) {
    FeatureBody {
        when {
            loading && analytics == null -> GlassLoading("در حال محاسبه آمار فروش…")
            error != null && analytics == null -> GlassEmptyState(error)
            analytics == null -> GlassEmptyState("هنوز داده‌ای برای نمایش تحلیل فروش آماده نیست.")
            else -> {
                RangeSelector(selectedRange, onRangeSelected)
                Spacer(Modifier.height(10.dp))
                OverviewCard(analytics, selectedRange)
                ComparisonCard(analytics)
                TrendSection(analytics)
                CompletionCard(analytics)
                StatusBreakdownCard(analytics)
                ProductBreakdownCard(analytics)
                CustomerInsightsCard(analytics)
                CouponPerformanceCard(analytics)
                InventoryHealthCard(analytics)
            }
        }
    }
}