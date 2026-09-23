package com.samanramezani1377.woogit.presentation.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.samanramezani1377.woogit.presentation.*
import kotlinx.coroutines.launch

@Composable
internal fun AiScreen(onOpenChatGpt: () -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val vm = viewModel<AiViewModel>(factory = AiViewModel.Factory(context))
    val state by vm.state.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val providerId by vm.providerId.collectAsState()
    val history by vm.history.collectAsState()
    val attachments by vm.attachments.collectAsState()

    var apiKey by remember(providerId) { mutableStateOf(vm.apiKey) }
    var geminiModel by remember { mutableStateOf(vm.geminiModel) }
    var groqModel by remember { mutableStateOf(vm.groqModel) }
    var cloudflareModel by remember { mutableStateOf(vm.cloudflareModel) }
    var cloudflareAccountId by remember { mutableStateOf(vm.cloudflareAccountId) }
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }
    var showMoreHistory by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            vm.addImage(uri)
        }
    }

    val messages = when (val value = state) {
        AiUiState.Idle -> emptyList()
        is AiUiState.Working -> value.messages
        is AiUiState.Ready -> value.messages
        is AiUiState.Error -> value.messages
    }
    val activities = (state as? AiUiState.Working)?.activities.orEmpty()
    val streamingText = (state as? AiUiState.Working)?.streamingText.orEmpty()
    val visibleHistory = history.take(if (showMoreHistory) 10 else 5)

    AiDrawer(
        vm = vm,
        drawerState = drawerState,
        scope = scope,
        visibleHistory = visibleHistory,
        historySize = history.size,
        showMoreHistory = showMoreHistory,
        onShowMoreHistoryChange = { showMoreHistory = it },
        onShowMemory = { showMemory = true },
    ) {
        GlassScaffold { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(6.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("WooGit AI", fontWeight = FontWeight.Bold)
                        Text(
                            "Agent داخلی WooGit · ${providerLabel(providerId)}",
                            color = GlassTokens.muted,
                        )
                    }

                    IconButton(
                        onClick = onOpenChatGpt,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassTokens.accent.copy(alpha = .14f)),
                    ) {
                        Text(
                            "↗",
                            color = GlassTokens.accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    IconButton(
                        onClick = {
                            apiKey = vm.apiKey
                            geminiModel = vm.geminiModel
                            groqModel = vm.groqModel
                            cloudflareModel = vm.cloudflareModel
                            cloudflareAccountId = vm.cloudflareAccountId
                            showSettings = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassTokens.accent.copy(alpha = .14f)),
                    ) {
                        Text(
                            "⚙",
                            color = GlassTokens.accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GlassTokens.accent.copy(alpha = .12f)),
                    ) {
                        Text(
                            "☰",
                            color = GlassTokens.accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (messages.isEmpty() && state !is AiUiState.Error) {
                    Spacer(Modifier.height(4.dp))
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(
                            "از Agent بخواهید روی فروشگاه کاری انجام دهد",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "مثلاً: محصول شماره ۱۲ را پیدا کن، یا محصولات ناموجود را فهرست کن.",
                            color = GlassTokens.muted,
                        )
                    }
                }

                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(messages) { index, message ->
                        MessageBubble(message) {
                            if (!isGenerating) {
                                vm.retry(index)
                            }
                        }
                    }

                    if (state is AiUiState.Working && activities.isNotEmpty()) {
                        item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Text("فعالیت Agent", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                activities.forEach { activity ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            if (activity.completed) "✓" else "●",
                                            color = if (activity.completed) {
                                                GlassTokens.live
                                            } else {
                                                GlassTokens.accent
                                            },
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            "  ${activity.text}",
                                            color = GlassTokens.muted,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (streamingText.isNotBlank()) {
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(.88f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = .54f))
                                        .padding(14.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "WooGit AI",
                                            color = GlassTokens.ink,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            streamingText + "▌",
                                            color = GlassTokens.ink,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val pending = (state as? AiUiState.Ready)?.pending
                    if (pending?.batchConfirmation != null) {
                        item {
                            BatchConfirmationCard(
                                pending = pending,
                                onConfirm = { ids -> vm.confirmBatch(pending, ids) },
                                onReject = { vm.reject(pending) },
                            )
                        }
                    } else if (pending != null) {
                        item {
                            GlassCard {
                                Text("تأیید عملیات", fontWeight = FontWeight.Bold)
                                Text(
                                    "AI می‌خواهد این تغییر را از طریق WooGit اجرا کند:",
                                    color = GlassTokens.muted,
                                )
                                Text(
                                    toolDisplayName(pending.toolName),
                                    color = GlassTokens.accent,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                val details = formatToolArguments(pending.toolArguments.orEmpty())
                                if (details.isNotBlank()) {
                                    Text(details, color = GlassTokens.muted)
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    GlassButton(
                                        "تأیید و اجرا",
                                        { vm.confirm(pending) },
                                        Modifier.weight(1f),
                                    )
                                    GlassOutlinedButton(
                                        "رد کردن",
                                        { vm.reject(pending) },
                                        Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    if (state is AiUiState.Error) {
                        item {
                            GlassCard {
                                Text(
                                    "خطا: ${(state as? AiUiState.Error)?.message.orEmpty()}",
                                    color = GlassTokens.urgent,
                                )
                            }
                        }
                    }
                }

                if (attachments.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = .42f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AsyncImage(
                                model = attachments.first().bytes,
                                contentDescription = "پیش‌نمایش ${attachments.first().name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(11.dp)),
                            )
                            Text(
                                attachments.first().name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                color = GlassTokens.ink,
                            )
                        }
                        TextButton(onClick = vm::removeImage) {
                            Text("حذف")
                        }
                    }
                }

                AiComposer(
                    input = input,
                    onInputChange = { input = it },
                    enabled = !isGenerating &&
                        (input.isNotBlank() || attachments.isNotEmpty()) &&
                        apiKey.isNotBlank(),
                    onPickImage = { imagePicker.launch("image/*") },
                    onSend = {
                        vm.send(input)
                        input = ""
                    },
                    onStop = vm::stopGeneration,
                    isGenerating = isGenerating,
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    AiSettingsSheet(
        show = showSettings,
        onDismiss = { showSettings = false },
        vm = vm,
        providerId = providerId,
        apiKey = apiKey,
        onApiKeyChange = { apiKey = it },
        geminiModel = geminiModel,
        onGeminiModelChange = { geminiModel = it },
        groqModel = groqModel,
        onGroqModelChange = { groqModel = it },
        cloudflareModel = cloudflareModel,
        onCloudflareModelChange = { cloudflareModel = it },
        cloudflareAccountId = cloudflareAccountId,
        onCloudflareAccountIdChange = { cloudflareAccountId = it },
    )

    AgentMemoryDebugSheet(
        context,
        showMemory,
        { showMemory = false },
    )
}

@Composable
private fun BatchConfirmationCard(
    pending: AgentReply,
    onConfirm: (Set<String>) -> Unit,
    onReject: () -> Unit,
) {
    val batch = pending.batchConfirmation ?: return
    var selected by remember(batch.token) {
        mutableStateOf(batch.items.map { it.id }.toSet())
    }

    val allSelected = selected.size == batch.items.size
    val noneSelected = selected.isEmpty()

    GlassCard {
        Text(batch.title, fontWeight = FontWeight.Bold)
        Text(batch.description, color = GlassTokens.muted)
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = { selected = batch.items.map { it.id }.toSet() },
                enabled = !allSelected,
            ) {
                Text("انتخاب همه")
            }
            TextButton(
                onClick = { selected = emptySet() },
                enabled = !noneSelected,
            ) {
                Text("پاک کردن انتخاب")
            }
        }

        Text(
            "موارد انتخاب‌شده: ${selected.size} از ${batch.items.size}",
            color = GlassTokens.accent,
            fontWeight = FontWeight.SemiBold,
        )
        if (noneSelected) {
            Spacer(Modifier.height(4.dp))
            Text(
                "هیچ موردی انتخاب نشده است. برای اجرا حداقل یک مورد را انتخاب کنید.",
                color = GlassTokens.urgent,
            )
        }
        Spacer(Modifier.height(4.dp))

        batch.items.forEach { item ->
            val checked = item.id in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = .28f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { value ->
                        selected = if (value) {
                            selected + item.id
                        } else {
                            selected - item.id
                        }
                    },
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        item.label,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.ink,
                    )
                    val details = formatToolArguments(item.arguments)
                    if (details.isNotBlank()) {
                        Text(
                            details,
                            color = GlassTokens.muted,
                            maxLines = 3,
                        )
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassButton(
                "تأیید موارد انتخاب‌شده",
                { onConfirm(selected) },
                Modifier.weight(1f),
                enabled = !noneSelected,
            )
            GlassOutlinedButton(
                "رد کردن همه",
                onReject,
                Modifier.weight(1f),
            )
        }
    }
}