package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class DashboardDestination {
    DASHBOARD,
    ORDERS,
    PRODUCTS,
    SETTINGS,
}

@Composable
internal fun DashboardFloatingNavigation(
    selected: DashboardDestination,
    onDestinationSelected: (DashboardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DashboardNavItem(
                label = "داشبورد",
                selected = selected == DashboardDestination.DASHBOARD,
                onClick = { onDestinationSelected(DashboardDestination.DASHBOARD) },
            )
            DashboardNavItem(
                label = "سفارش‌ها",
                selected = selected == DashboardDestination.ORDERS,
                onClick = { onDestinationSelected(DashboardDestination.ORDERS) },
            )
            DashboardNavItem(
                label = "محصولات",
                selected = selected == DashboardDestination.PRODUCTS,
                onClick = { onDestinationSelected(DashboardDestination.PRODUCTS) },
            )
            DashboardNavItem(
                label = "تنظیمات",
                selected = selected == DashboardDestination.SETTINGS,
                onClick = { onDestinationSelected(DashboardDestination.SETTINGS) },
            )
        }
    }
}

@Composable
private fun DashboardNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(label)
    }
}
