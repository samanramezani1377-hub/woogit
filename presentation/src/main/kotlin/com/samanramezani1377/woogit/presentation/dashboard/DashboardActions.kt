package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dashboard quick-action section.
 * Navigation callbacks are supplied by the screen instead of being hard-coded here.
 */
@Composable
internal fun DashboardActions(
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "اقدامات سریع",
            style = MaterialTheme.typography.titleMedium,
        )

        DashboardQuickAction(
            title = "سفارش‌ها",
            subtitle = "مدیریت سفارش‌های فروشگاه",
            onClick = onOrdersClick,
        )

        DashboardQuickAction(
            title = "محصولات",
            subtitle = "مدیریت محصولات و موجودی",
            onClick = onProductsClick,
        )
    }
}
