package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class VariationUiModel(
    val id: String,
    val title: String,
    val price: String,
    val stock: String,
)

@Composable
internal fun VariationsScreen(
    variations: List<VariationUiModel>,
    onVariationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("تنوع‌ها")

        variations.forEach { variation ->
            Button(
                onClick = { onVariationClick(variation.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(variation.title)
                    Text("${variation.price} · ${variation.stock}")
                }
            }
        }
    }
}
