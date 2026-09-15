package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun CouponsPage(state: CommerceUiState) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = state.coupons.filter { it.code.contains(query.trim(), true) }

    Box(Modifier.fillMaxSize()) {
        FeatureBody {
            Section("کوپن‌ها", "کوپن‌های فروشگاه را جستجو و بررسی کنید.") {
                GlassSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = "کد کوپن",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${visible.size} کوپن",
                    color = GlassTokens.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (visible.isEmpty()) {
                GlassEmptyState("کوپنی مطابق جستجو پیدا نشد.")
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.id }) { coupon ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(coupon.code, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "مبلغ: ${coupon.amount}",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(coupon.usage_count.toString(), fontWeight = FontWeight.Bold)
                                    Text(
                                        "استفاده",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
