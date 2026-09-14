package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class CommerceFeature {
    BARCODE,
    BULK_ORDERS,
    INVENTORY,
    CUSTOMERS,
    ANALYTICS,
    COUPONS,
    INVOICE,
}

private data class CommerceFeatureUiModel(
    val feature: CommerceFeature,
    val title: String,
    val description: String,
)

@Composable
internal fun CommerceCenterScreen(
    onFeatureSelected: (CommerceFeature) -> Unit,
    modifier: Modifier = Modifier,
) {
    val features = listOf(
        CommerceFeatureUiModel(
            CommerceFeature.BARCODE,
            "اسکن بارکد و SKU",
            "پیدا کردن سریع محصول، SKU یا سفارش با دوربین.",
        ),
        CommerceFeatureUiModel(
            CommerceFeature.BULK_ORDERS,
            "عملیات گروهی سفارش‌ها",
            "انتخاب چند سفارش و تغییر وضعیت آن‌ها به صورت یکجا.",
        ),
        CommerceFeatureUiModel(
            CommerceFeature.INVENTORY,
            "مرکز موجودی",
            "فیلتر موجودی، کمبود موجودی و کالاهای ناموجود.",
        ),
        CommerceFeatureUiModel(
            CommerceFeature.CUSTOMERS,
            "مدیریت مشتریان",
            "مشاهده، جستجو، ایجاد و ویرایش مشتریان WooCommerce.",
        ),
        CommerceFeatureUiModel(
            CommerceFeature.ANALYTICS,
            "تحلیل فروش",
            "فروش، سفارش‌های تکمیل‌شده، میانگین سفارش و رتبه‌بندی‌ها.",
        ),
        CommerceFeatureUiModel(
            CommerceFeature.COUPONS,
            "مدیریت کوپن‌ها",
            "ایجاد، ویرایش، حذف و عملیات گروهی روی کوپن‌ها.",
        ),
        CommerceFeatureUiModel(
            CommerceFeature.INVOICE,
            "فاکتور و رسید PDF",
            "ساخت فاکتور از اطلاعات واقعی سفارش و ذخیره/اشتراک‌گذاری PDF.",
        ),
    )

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "ابزارهای فروشگاه",
            modifier = Modifier.padding(20.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(features) { item ->
                CommerceFeatureCard(
                    item = item,
                    onClick = { onFeatureSelected(item.feature) },
                )
            }
        }
    }
}

@Composable
private fun CommerceFeatureCard(
    item: CommerceFeatureUiModel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = item.title)
        Text(text = item.description)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onClick) {
                Text("باز کردن")
            }
        }
    }
}
