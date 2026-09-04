package com.samanramezani1377.woogit.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.presentation.GlassButton
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun AiScreen() {
    val vm = viewModel<AiViewModel>(factory = AiViewModel.Factory(LocalContext.current.applicationContext))
    val state by vm.state.collectAsState()
    var backendUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var showConnection by remember { mutableStateOf(false) }

    GlassScaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WooGit AI", fontWeight = FontWeight.Bold)
                    Text("دستیار هوشمند فروشگاه", color = GlassTokens.muted)
                }
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(GlassTokens.accent.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center,
                ) { Text("AI", color = GlassTokens.accent, fontWeight = FontWeight.Bold) }
            }

            GlassCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("اتصال به Agent", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (backendUrl.isBlank()) "Backend تنظیم نشده" else "Backend آماده اتصال",
                            color = if (backendUrl.isBlank()) GlassTokens.faint else GlassTokens.live,
                        )
                    }
                    GlassOutlinedButton(
                        label = if (showConnection) "بستن" else "تنظیمات",
                        onClick = { showConnection = !showConnection },
                        modifier = Modifier.widthIn(max = 120.dp),
                    )
                }
                if (showConnection) {
                    Spacer(Modifier.height(4.dp))
                    AiField(backendUrl, { backendUrl = it }, "آدرس Backend")
                    AiField(apiKey, { apiKey = it }, "کلید API", secret = true)
                    GlassButton("ذخیره اتصال", { vm.saveConnection(backendUrl, apiKey) })
                }
            }

            val messages = when (state) {
                AiUiState.Idle, AiUiState.Sending -> emptyList()
                is AiUiState.Ready -> (state as AiUiState.Ready).messages
                is AiUiState.Error -> (state as AiUiState.Error).messages
            }

            if (messages.isEmpty() && state !is AiUiState.Error) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("از AI بخواهید چه کاری برای فروشگاه انجام دهد", fontWeight = FontWeight.SemiBold)
                    Text("مثلاً: محصولات ناموجود را پیدا کن یا محصول شماره ۱۲ را بررسی کن.", color = GlassTokens.muted)
                }
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages) { message ->
                    val user = message.role == "user"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                        Box(
                            Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(20.dp)).background(
                                if (user) GlassTokens.accent.copy(alpha = .12f) else Color.White.copy(alpha = .54f)
                            ).padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (user) "شما" else "WooGit AI", color = if (user) GlassTokens.accent else GlassTokens.ink, fontWeight = FontWeight.SemiBold)
                                Text(message.content, color = GlassTokens.ink)
                            }
                        }
                    }
                }
                val pending = (state as? AiUiState.Ready)?.pending
                if (pending != null) {
                    item {
                        GlassCard {
                            Text("تأیید عملیات", fontWeight = FontWeight.Bold)
                            Text("AI می‌خواهد این عملیات را اجرا کند:", color = GlassTokens.muted)
                            Text(pending.toolName.orEmpty(), color = GlassTokens.accent, fontWeight = FontWeight.SemiBold)
                            Text(pending.toolArguments.orEmpty(), color = GlassTokens.muted)
                            GlassButton("تأیید و اجرا", { vm.confirm(pending) })
                        }
                    }
                }
                if (state is AiUiState.Error) {
                    item {
                        GlassCard { Text("خطا: ${(state as AiUiState.Error).message}", color = GlassTokens.urgent) }
                    }
                }
            }

            if (state is AiUiState.Sending) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = GlassTokens.accent)
                }
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .60f)).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiField(input, { input = it }, "دستور به AI", Modifier.weight(1f), singleLine = false)
                IconButton(onClick = { vm.send(input); input = "" }, enabled = input.isNotBlank() && state !is AiUiState.Sending) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(GlassTokens.accent), contentAlignment = Alignment.Center) {
                        Text("↑", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AiField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    singleLine: Boolean = true,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = GlassTokens.accent,
            unfocusedIndicatorColor = GlassTokens.glassBorder,
        ),
    )
}
