package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun FeatureHeader(title: String, subtitle: String, onBack: () -> Unit) {
    GlassTopBar(title = title, subtitle = subtitle, actions = { GlassOutlinedButton("بازگشت", onBack) }, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
}

@Composable
internal fun FeatureBody(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable
internal fun Section(title: String, description: String? = null, content: @Composable () -> Unit) {
    GlassCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        content()
    }
}

@Composable
internal fun SelectionSummary(count: Int, total: Int) {
    GlassCard(Modifier.fillMaxWidth()) { Text("$count انتخاب از $total", fontWeight = FontWeight.SemiBold) }
}

internal fun CommerceFeature.titleFa(): String = when (this) {
    CommerceFeature.CUSTOMERS -> "مدیریت مشتریان"
    CommerceFeature.COUPONS -> "مدیریت کوپن‌ها"
}

internal fun CommerceFeature.subtitleFa(): String = when (this) {
    CommerceFeature.CUSTOMERS -> "فهرست، پرونده و سابقه خرید مشتریان"
    CommerceFeature.COUPONS -> "پیدا کردن و ویرایش کوپن‌ها"
}

internal fun OrderStatus.faLabel(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.ON_HOLD -> "در انتظار بررسی"
    OrderStatus.COMPLETED -> "تکمیل‌شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.OTHER -> "سایر"
}
