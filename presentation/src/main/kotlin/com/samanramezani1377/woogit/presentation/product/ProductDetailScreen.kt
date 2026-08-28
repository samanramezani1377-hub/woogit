package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun ProductDetailScreen(product: Product, onBack: () -> Unit, onEdit: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassTopBar(title = product.name, subtitle = "جزئیات محصول")
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassText("وضعیت: ${product.status}")
                GlassText("قیمت: ${product.pricing.sale ?: product.pricing.regular ?: "—"}")
                GlassText(
                    product.stock?.quantity?.toString()?.removeSuffix(".0")?.let { "موجودی: $it" } ?: "موجودی نامشخص",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = GlassTokens.muted),
                )
            }
        }
        com.samanramezani1377.woogit.presentation.GlassPrimaryAction("ویرایش محصول", onEdit, Modifier.fillMaxWidth())
        com.samanramezani1377.woogit.presentation.GlassPrimaryAction("بازگشت", onBack, Modifier.fillMaxWidth())
    }
}
