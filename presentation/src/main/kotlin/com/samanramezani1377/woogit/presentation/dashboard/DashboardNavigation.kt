package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassTokens

internal enum class DashboardDestination { DASHBOARD, ORDERS, PRODUCTS, SETTINGS }

@Composable
internal fun DashboardFloatingNavigation(
    selected: DashboardDestination,
    onDestinationSelected: (DashboardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = .72f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder),
        shadowElevation = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DashboardNavItem("داشبورد", selected == DashboardDestination.DASHBOARD) { onDestinationSelected(DashboardDestination.DASHBOARD) }
            DashboardNavItem("سفارش‌ها", selected == DashboardDestination.ORDERS) { onDestinationSelected(DashboardDestination.ORDERS) }
            DashboardNavItem("محصولات", selected == DashboardDestination.PRODUCTS) { onDestinationSelected(DashboardDestination.PRODUCTS) }
            DashboardNavItem("تنظیمات", selected == DashboardDestination.SETTINGS) { onDestinationSelected(DashboardDestination.SETTINGS) }
        }
    }
}

@Composable
private fun DashboardNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 1.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) GlassTokens.accent else GlassTokens.muted,
            containerColor = if (selected) GlassTokens.accent.copy(alpha = .12f) else Color.Transparent,
        ),
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
    }
}
