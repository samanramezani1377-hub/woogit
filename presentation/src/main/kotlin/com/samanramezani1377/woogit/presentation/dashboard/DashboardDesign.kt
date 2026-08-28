package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun DashboardHero(storeName: String, connected: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (connected) GlassTokens.live else GlassTokens.urgent, CircleShape),
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("وضعیت فروشگاه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(storeName, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.muted)
            }
            Text(
                if (connected) "متصل" else "قطع ارتباط",
                color = if (connected) GlassTokens.live else GlassTokens.urgent,
                fontWeight = FontWeight.Bold,
            )
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
    GlassCard(modifier) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.muted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DashboardQuickAction(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassTokens.glassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = .28f),
            contentColor = GlassTokens.ink,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = GlassTokens.ink, fontWeight = FontWeight.Bold)
                Text(subtitle, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("‹", color = GlassTokens.faint, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun DashboardContent(
    storeName: String,
    connected: Boolean,
    orders: String,
    products: String,
    revenue: String,
    pending: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "خلاصه وضعیت",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        DashboardHero(storeName, connected)
        DashboardStatGrid(orders, products, revenue, pending)
    }
}
