package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class AttributeUiModel(
    val id: String,
    val name: String,
    val value: String,
)

@Composable
internal fun AttributesScreen(
    attributes: List<AttributeUiModel>,
    onAttributeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ویژگی‌ها")

        attributes.forEach { attribute ->
            Button(
                onClick = { onAttributeClick(attribute.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${attribute.name}: ${attribute.value}")
            }
        }
    }
}
