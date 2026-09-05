package com.samanramezani1377.woogit.presentation.ai

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.presentation.*
import kotlinx.coroutines.launch

@Composable
internal fun AiScreen() {
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
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var groqMenuExpanded by remember { mutableStateOf(false) }
    var cloudflareMenuExpanded by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.addImage(uri) }
    val messages = when (val value = state) {
        AiUiState.Idle -> emptyList()
        is AiUiState.Working -> value.messages
        is AiUiState.Ready -> value.messages
        is AiUiState.Error -> value.messages
    }
    val activities = (state as? AiUiState.Working)?.activities.orEmpty()
    val streamingText = (state as? AiUiState.Working)?.streamingText.orEmpty()
    val visibleHistory = history.take(if (showMoreHistory) 10 else 5)

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(modifier = Modifier.fillMaxWidth(.86f), drawerShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp), drawerContainerColor = Color.White.copy(alpha = .96f)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(18.dp))
                Text("گفتگوها", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                GlassButton("＋  چت جدید", { vm.newChat(); showMoreHistory = false; scope.launch { drawerState.close() } }, Modifier.fillMaxWidth())
                if (SHOW_AGENT_MEMORY_DEBUG) {
                    Spacer(Modifier.height(8.dp))
                    GlassOutlinedButton("🧠  حافظه Agent", { showMemory = true }, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GlassTokens.glassBorder)
                Spacer(Modifier.height(14.dp))
                Text("آخرین چت‌ها", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(visibleHistory, key = { it.id }) { session -> HistoryItem(session) { vm.openChat(session.id); scope.launch { drawerState.close() } } } }
                if (history.size > 5) { Spacer(Modifier.height(10.dp)); GlassOutlinedButton(if (showMoreHistory) "نمایش ۵ چت اخیر" else "مشاهده بیشتر", { showMoreHistory = !showMoreHistory }, Modifier.fillMaxWidth()) }
                Spacer(Modifier.height(18.dp))
            }
        }
    }) {
        GlassScaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("WooGit AI", fontWeight = FontWeight.Bold); Text("Agent داخلی WooGit · ${providerLabel(providerId)}", color = GlassTokens.muted) }
                    IconButton(onClick = { apiKey = vm.apiKey; geminiModel = vm.geminiModel; groqModel = vm.groqModel; cloudflareModel = vm.cloudflareModel; cloudflareAccountId = vm.cloudflareAccountId; showSettings = true }, modifier = Modifier.size(44.dp).clip(CircleShape).background(GlassTokens.accent.copy(alpha = .14f))) { Text("⚙", color = GlassTokens.accent, fontWeight = FontWeight.Bold) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(46.dp).clip(CircleShape).background(GlassTokens.accent.copy(alpha = .12f))) { Text("☰", color = GlassTokens.accent, fontWeight = FontWeight.Bold) } }
                if (messages.isEmpty() && state !is AiUiState.Error) { Spacer(Modifier.height(4.dp)); GlassCard(Modifier.fillMaxWidth()) { Text("از Agent بخواهید روی فروشگاه کاری انجام دهد", fontWeight = FontWeight.SemiBold); Text("مثلاً: محصول شماره ۱۲ را پیدا کن، یا محصولات ناموجود را فهرست کن.", color = GlassTokens.muted) } }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages) { message -> MessageBubble(message) }
                    if (state is AiUiState.Working && activities.isNotEmpty()) item { GlassCard(Modifier.fillMaxWidth()) { Text("فعالیت Agent", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); activities.forEach { activity -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (activity.completed) "✓" else "●", color = if (activity.completed) GlassTokens.live else GlassTokens.accent, fontWeight = FontWeight.Bold); Text("  ${activity.text}", color = GlassTokens.muted) } } } }
                    if (streamingText.isNotBlank()) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { Box(Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .54f)).padding(14.dp)) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("WooGit AI", color = GlassTokens.ink, fontWeight = FontWeight.SemiBold); Text(streamingText + "▌", color = GlassTokens.ink) } } } }
                    val pending = (state as? AiUiState.Ready)?.pending
                    if (pending != null) item { GlassCard { Text("تأیید عملیات", fontWeight = FontWeight.Bold); Text("AI می‌خواهد این تغییر را از طریق WooGit اجرا کند:", color = GlassTokens.muted); Text(pending.toolName.orEmpty(), color = GlassTokens.accent, fontWeight = FontWeight.SemiBold); Text(pending.toolArguments.orEmpty(), color = GlassTokens.muted); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { GlassButton("تأیید و اجرا", { vm.confirm(pending) }, Modifier.weight(1f)); GlassOutlinedButton("رد کردن", { vm.reject(pending) }, Modifier.weight(1f)) } } }
                    if (state is AiUiState.Error) item { GlassCard { Text("خطا: ${(state as AiUiState.Error).message}", color = GlassTokens.urgent) } }
                }
                if (attachments.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { GlassOutlinedButton("🖼  ${attachments.first().name}", {}, Modifier.weight(1f)); TextButton(onClick = vm::removeImage) { Text("حذف") } }
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .60f)).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imagePicker.launch("image/*") }, enabled = !isGenerating) { Text("+", color = GlassTokens.accent, fontWeight = FontWeight.Bold) }
                    AiField(input, { input = it }, "دستور به AI", Modifier.weight(1f), singleLine = false)
                    if (isGenerating) {
                        IconButton(onClick = vm::stopGeneration, modifier = Modifier.size(52.dp)) { Box(Modifier.size(44.dp).clip(CircleShape).background(GlassTokens.urgent), contentAlignment = Alignment.Center) { Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color.White)) } }
                    } else {
                        IconButton(onClick = { vm.send(input); input = "" }, enabled = (input.isNotBlank() || attachments.isNotEmpty()) && apiKey.isNotBlank()) { Box(Modifier.size(44.dp).clip(CircleShape).background(GlassTokens.accent), contentAlignment = Alignment.Center) { Text("↑", color = Color.White, fontWeight = FontWeight.Bold) } }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    GlassBottomSheet(show = showSettings, onDismiss = { showSettings = false }) {
        Text("تنظیمات AI", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("سرویس AI", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            GlassOutlinedButton("${providerLabel(providerId)}  ·  ${providerModelLabel(providerId, geminiModel, groqModel, cloudflareModel)}", { providerMenuExpanded = true }, Modifier.fillMaxWidth())
            DropdownMenu(expanded = providerMenuExpanded, onDismissRequest = { providerMenuExpanded = false }) {
                AI_PROVIDERS.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(providerLabel(provider), fontWeight = if (provider == providerId) FontWeight.SemiBold else FontWeight.Normal)
                                Text(providerModelLabel(provider, geminiModel, groqModel, cloudflareModel), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        leadingIcon = { Text(if (provider == providerId) "✓" else "", color = GlassTokens.accent, fontWeight = FontWeight.Bold) },
                        onClick = {
                            vm.selectProvider(provider)
                            apiKey = vm.apiKey
                            if (provider == "gemini") geminiModel = vm.geminiModel
                            if (provider == "groq") groqModel = vm.groqModel
                            if (provider == "cloudflare") { cloudflareModel = vm.cloudflareModel; cloudflareAccountId = vm.cloudflareAccountId }
                            providerMenuExpanded = false
                        },
                    )
                }
            }
        }
        Text("ارائه‌دهنده را از یک کنترل واحد انتخاب کنید؛ مدل فعال همزمان زیر همین انتخاب مشخص است.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)

        if (providerId == "gemini") { Spacer(Modifier.height(10.dp)); Text("مدل Gemini", fontWeight = FontWeight.SemiBold); AiField(geminiModel, { geminiModel = it }, "Model ID (مثلاً gemini-3.8-flash)"); Text("شناسه مدل مستقیماً از تنظیمات خوانده می‌شود؛ با تغییر مدل نیازی به تغییر Agent نیست.", color = GlassTokens.muted); Spacer(Modifier.height(4.dp)) }
        if (providerId == "groq") {
            Spacer(Modifier.height(10.dp)); Text("مدل Groq", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Box { GlassOutlinedButton(groqModel, { groqMenuExpanded = true }, Modifier.fillMaxWidth()); DropdownMenu(expanded = groqMenuExpanded, onDismissRequest = { groqMenuExpanded = false }) { GROQ_MODELS.forEach { model -> DropdownMenuItem(text = { Text(model) }, onClick = { groqModel = model; groqMenuExpanded = false }) } } }
            Text("برای پیام متنی مدل انتخاب‌شده استفاده می‌شود. هنگام ارسال تصویر، Groq به‌صورت خودکار از Qwen3.6-27B Vision استفاده می‌کند.", color = GlassTokens.muted); Spacer(Modifier.height(4.dp))
        }
        if (providerId == "cloudflare") {
            Spacer(Modifier.height(10.dp)); Text("Cloudflare Account ID", fontWeight = FontWeight.SemiBold); AiField(cloudflareAccountId, { cloudflareAccountId = it }, "Account ID")
            Spacer(Modifier.height(8.dp)); Text("مدل Cloudflare", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Box { GlassOutlinedButton(cloudflareModel, { cloudflareMenuExpanded = true }, Modifier.fillMaxWidth()); DropdownMenu(expanded = cloudflareMenuExpanded, onDismissRequest = { cloudflareMenuExpanded = false }) { CLOUDFLARE_MODELS.forEach { model -> DropdownMenuItem(text = { Text(model) }, onClick = { cloudflareModel = model; cloudflareMenuExpanded = false }) } } }
            Text("مدل انتخاب‌شده برای متن استفاده می‌شود؛ هنگام ارسال تصویر، Agent از مدل Vision مربوط به Cloudflare استفاده می‌کند.", color = GlassTokens.muted); Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${providerLabel(providerId)} API", fontWeight = FontWeight.SemiBold); Text(if (apiKey.isBlank()) "کلید تنظیم نشده" else "کلید روی دستگاه ذخیره شده است", color = if (apiKey.isBlank()) GlassTokens.faint else GlassTokens.live) }; Text("مستقیم", color = GlassTokens.muted) }
        Spacer(Modifier.height(4.dp)); AiField(apiKey, { apiKey = it }, if (providerId == "cloudflare") "API Token کلادفلر" else "کلید API ${providerLabel(providerId)}", secret = true); Text(providerDescription(providerId), color = GlassTokens.muted); Spacer(Modifier.height(4.dp)); GlassButton("ذخیره تنظیمات", { vm.saveApiKey(apiKey); if (providerId == "gemini") vm.saveGeminiModel(geminiModel); if (providerId == "groq") vm.saveGroqModel(groqModel); if (providerId == "cloudflare") { vm.saveCloudflareModel(cloudflareModel); vm.saveCloudflareAccountId(cloudflareAccountId) }; showSettings = false })
    }

    AgentMemoryDebugSheet(context, showMemory, { showMemory = false })
}

private val AI_PROVIDERS = listOf("gemini", "openrouter", "deepseek", "groq", "cloudflare")
private val GROQ_MODELS = listOf("openai/gpt-oss-20b", "openai/gpt-oss-120b", "qwen/qwen3.6-27b", "qwen/qwen3.8-27b", "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "minimaxai/minimax-m2.7")
private val CLOUDFLARE_MODELS = listOf("@cf/zai-org/glm-4.7-flash", "@cf/google/gemma-4-26b-a4b-it")

private fun providerLabel(id: String) = when (id) { "gemini" -> "Gemini"; "deepseek" -> "DeepSeek"; "groq" -> "Groq"; "cloudflare" -> "Cloudflare"; else -> "OpenRouter" }
private fun providerModelLabel(id: String, geminiModel: String, groqModel: String, cloudflareModel: String) = when (id) { "gemini" -> geminiModel; "deepseek" -> "deepseek-v4-flash"; "groq" -> groqModel; "cloudflare" -> cloudflareModel; else -> "openrouter/free" }
private fun providerDescription(id: String) = when (id) { "gemini" -> "اتصال مستقیم به Google Gemini API؛ مدل انتخاب‌شده با tool calling استفاده می‌شود."; "deepseek" -> "اتصال مستقیم به api.deepseek.com؛ Backend جداگانه لازم نیست."; "groq" -> "اتصال مستقیم به Groq API؛ مدل انتخاب‌شده برای متن و Qwen3.6-27B برای ورودی تصویر استفاده می‌شود."; "cloudflare" -> "اتصال مستقیم به Workers AI REST API؛ Account ID و API Token لازم است."; else -> "اتصال مستقیم به OpenRouter؛ روتر openrouter/free مدل مناسب را انتخاب می‌کند." }
@Composable private fun HistoryItem(session: AiChatSession, onClick: () -> Unit) { GlassOutlinedButton(session.title, onClick, Modifier.fillMaxWidth()) }
@Composable private fun MessageBubble(message: AiMessage) { val user = message.role == "user"; Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { Box(Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(20.dp)).background(if (user) GlassTokens.accent.copy(alpha = .12f) else Color.White.copy(alpha = .54f)).padding(14.dp)) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (user) "شما" else "WooGit AI", color = if (user) GlassTokens.accent else GlassTokens.ink, fontWeight = FontWeight.SemiBold); message.attachment?.let { attachment -> val bitmap = remember(attachment) { BitmapFactory.decodeByteArray(attachment.bytes, 0, attachment.bytes.size)?.asImageBitmap() }; bitmap?.let { Image(it, contentDescription = attachment.name, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(14.dp))) } }; if (message.content.isNotBlank()) Text(message.content, color = GlassTokens.ink) } } } }
@Composable private fun AiField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, secret: Boolean = false, singleLine: Boolean = true) { TextField(value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = GlassTokens.accent, unfocusedIndicatorColor = GlassTokens.glassBorder)) }
