package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Surface = Color.White.copy(alpha = 0.58f)

@Composable
internal fun DashboardGlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(26.dp)).background(Surface).padding(18.dp),
        content = content,
    )
}

@Composable
internal fun DashboardHero(storeName: String, connected: Boolean, modifier: Modifier = Modifier) {
    DashboardGlassSurface(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(if (connected) Color(0xFF25B36A) else Color(0xFFE67E22)))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("WooGit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(storeName, style = MaterialTheme.typography.bodyMedium)
            }
            Text(if (connected) "متصل" else "قطع ارتباط", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun DashboardStatGrid(orders: String, products: String, revenue: String, pending: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStat("سفارش‌ها", orders, Modifier.weight(1f))
            DashboardStat("محصولات", products, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStat("فروش", revenue, Modifier.weight(1f))
            DashboardStat("در انتظار", pending, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardStat(title: String, value: String, modifier: Modifier = Modifier) {
    DashboardGlassSurface(modifier) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DashboardQuickAction(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun DashboardContent(
    storeName: String, connected: Boolean, orders: String, products: String,
    revenue: String, pending: String, modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFEFF1F7)).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DashboardHero(storeName, connected)
        Text("خلاصه وضعیت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        DashboardStatGrid(orders, products, revenue, pending)
        DashboardGlassSurface(Modifier.fillMaxWidth()) {
            Text("اقدامات سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            DashboardQuickAction("سفارش‌ها", "مدیریت سفارش‌های فروشگاه", onClick = {})
            Spacer(Modifier.height(8.dp))
            DashboardQuickAction("محصولات", "مدیریت محصولات و موجودی", onClick = {})
        }
    }
}
