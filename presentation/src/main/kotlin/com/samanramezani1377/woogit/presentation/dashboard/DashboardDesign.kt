package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun DashboardHero(storeName: String, connected: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (connected) GlassTokens.live else GlassTokens.urgent, CircleShape))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("وضعیت فروشگاه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(storeName, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.muted)
            }
            Text(if (connected) "متصل" else "قطع ارتباط", color = if (connected) GlassTokens.live else GlassTokens.urgent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun DashboardRecentOrder(orderId: String?, customer: String, total: String, status: OrderStatus?, onClick: () -> Unit) {
    if (orderId == null) return
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("آخرین سفارش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                status?.let { GlassStatusBadge(it.dashboardLabel()) }
            }
            GlassText("#$orderId · ${customer.ifBlank { "مشتری نامشخص" }}")
            GlassText(total)
            DashboardQuickAction("مشاهده سفارش", "مشاهده جزئیات و مدیریت", onClick = onClick)
        }
    }
}

@Composable
internal fun DashboardStatGrid(orders: String, products: String, revenue: String, pending: String, currency: String = "تومان") {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStat("سفارش‌ها", orders, Modifier.weight(1f))
            DashboardStat("محصولات", products, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStat("فروش ($currency)", revenue, Modifier.weight(1f))
            DashboardStat("در انتظار", pending, Modifier.weight(1f))
        }
    }
}

@Composable private fun DashboardStat(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.muted)
        Spacer(Modifier.size(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DashboardQuickAction(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 68.dp).shadow(7.dp, shape, ambientColor = GlassTokens.ink.copy(alpha = .07f), spotColor = GlassTokens.ink.copy(alpha = .08f)),
        shape = shape,
        border = BorderStroke(1.dp, GlassTokens.glassBorder),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = .40f), contentColor = GlassTokens.ink),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = GlassTokens.ink, fontWeight = FontWeight.Bold)
                Text(subtitle, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("‹", color = GlassTokens.faint, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun DashboardContent(
    storeName: String, connected: Boolean, orders: String, products: String, revenue: String, pending: String,
    recentOrderId: String?, recentCustomer: String, recentTotal: String, recentStatus: OrderStatus?, onRecentOrderClick: () -> Unit,
    currency: String = "تومان", modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("خلاصه وضعیت", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        DashboardHero(storeName, connected)
        DashboardStatGrid(orders, products, revenue, pending, currency)
        DashboardRecentOrder(recentOrderId, recentCustomer, recentTotal, recentStatus, onRecentOrderClick)
    }
}

private fun OrderStatus.dashboardLabel(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.COMPLETED -> "تکمیل شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.ON_HOLD -> "در انتظار"
    OrderStatus.OTHER -> "سایر"
}
