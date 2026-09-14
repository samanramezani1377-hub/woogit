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

@Composable
internal fun DashboardActions(
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
    onConflictsClick: () -> Unit,
    onCommerceClick: () -> Unit = {},
    onBarcodeClick: () -> Unit = onCommerceClick,
    onAnalyticsClick: () -> Unit = onCommerceClick,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("اقدامات سریع", style = MaterialTheme.typography.titleMedium)
        DashboardQuickAction("سفارش‌ها", "مدیریت سفارش‌های فروشگاه", onClick = onOrdersClick)
        DashboardQuickAction("محصولات", "مدیریت محصولات و موجودی", onClick = onProductsClick)
        DashboardQuickAction("بارکد و SKU", "جستجوی سریع محصول یا سفارش با بارکد و SKU", onClick = onBarcodeClick)
        DashboardQuickAction("تحلیل فروش", "بررسی فروش، درآمد، سفارش‌ها و شاخص‌های عملکرد فروشگاه", onClick = onAnalyticsClick)
        DashboardQuickAction("تجارت", "مدیریت مشتریان و کوپن‌ها", onClick = onCommerceClick)
        DashboardQuickAction("همگام‌سازی", "همگام‌سازی داده‌های فروشگاه", onClick = onSyncClick)
        DashboardQuickAction("تعارض‌ها", "بررسی و حل تعارض‌های داده", onClick = onConflictsClick)
        DashboardQuickAction("تنظیمات فروشگاه", "اتصال، حساب و مدیریت فروشگاه", onClick = onSettingsClick)
    }
}
