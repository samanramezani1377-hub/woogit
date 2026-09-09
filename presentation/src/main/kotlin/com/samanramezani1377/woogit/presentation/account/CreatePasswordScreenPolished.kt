package com.samanramezani1377.woogit.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun PasswordRequirementsCard(
    password: String,
    confirmation: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            GlassText("الزامات رمز عبور", fontWeight = FontWeight.SemiBold)
            PasswordRequirement("حداقل ۸ کاراکتر", password.length >= 8)
            PasswordRequirement(
                "تکرار رمز عبور یکسان باشد",
                confirmation.isNotEmpty() && password == confirmation,
            )
        }
    }
}

@Composable
private fun PasswordRequirement(label: String, satisfied: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlassText(label)
        GlassText(
            if (satisfied) "✓" else "—",
            color = if (satisfied) GlassTokens.live else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}
