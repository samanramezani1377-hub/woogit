package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun <T> GlassDropdown(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, onSelected: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        GlassOutlinedButton(optionLabel(selected), { expanded = true }, Modifier.fillMaxWidth(), enabled)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { expanded = false; onSelected(option) })
            }
        }
    }
}

@Composable
fun GlassCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp)) { Checkbox(checked, onCheckedChange); Text(label, Modifier.padding(top = 12.dp)) }

@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label, Modifier.weight(1f).padding(top = 12.dp)); Switch(checked, onCheckedChange) }

@Composable
fun GlassRadioButton(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp)) { RadioButton(selected, onClick); Text(label, Modifier.padding(top = 12.dp)) }

@Composable
fun GlassChip(label: String, modifier: Modifier = Modifier) =
    AssistChip(
        onClick = {}, label = { Text(label) }, modifier = modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Color.White.copy(alpha = .65f))
    )
