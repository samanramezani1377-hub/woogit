package com.samanramezani1377.woogit.presentation.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.*

@Composable
internal fun AiSettingsSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    vm: AiViewModel,
    providerId: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    geminiModel: String,
    onGeminiModelChange: (String) -> Unit,
    groqModel: String,
    onGroqModelChange: (String) -> Unit,
    cloudflareModel: String,
    onCloudflareModelChange: (String) -> Unit,
    cloudflareAccountId: String,
    onCloudflareAccountIdChange: (String) -> Unit,
) {
    varProviderMenu(show, onDismiss, vm, providerId, apiKey, onApiKeyChange, geminiModel, onGeminiModelChange, groqModel, onGroqModelChange, cloudflareModel, onCloudflareModelChange, cloudflareAccountId, onCloudflareAccountIdChange)
}

@Composable
private fun varProviderMenu(
    show: Boolean, onDismiss: () -> Unit, vm: AiViewModel, providerId: String, apiKey: String,
    onApiKeyChange: (String) -> Unit, geminiModel: String, onGeminiModelChange: (String) -> Unit,
    groqModel: String, onGroqModelChange: (String) -> Unit, cloudflareModel: String,
    onCloudflareModelChange: (String) -> Unit, cloudflareAccountId: String, onCloudflareAccountIdChange: (String) -> Unit,
) {
    var providerMenuExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var groqMenuExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var cloudflareMenuExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    GlassBottomSheet(show = show, onDismiss = onDismiss) {
        Text("تنظیمات AI", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ارائه‌دهنده و مدل", fontWeight = FontWeight.Bold)
                Text("ابتدا سرویس را انتخاب کنید، سپس مدل همان سرویس را تنظیم کنید.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("سرویس AI", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            GlassOutlinedButton("${providerLabel(providerId)}  ·  ${providerModelLabel(providerId, geminiModel, groqModel, cloudflareModel)}", { providerMenuExpanded.value = true }, Modifier.fillMaxWidth())
            DropdownMenu(expanded = providerMenuExpanded.value, onDismissRequest = { providerMenuExpanded.value = false }) {
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
                            onApiKeyChange(vm.apiKey)
                            if (provider == "gemini") onGeminiModelChange(vm.geminiModel)
                            if (provider == "groq") onGroqModelChange(vm.groqModel)
                            if (provider == "cloudflare") { onCloudflareModelChange(vm.cloudflareModel); onCloudflareAccountIdChange(vm.cloudflareAccountId) }
                            providerMenuExpanded.value = false
                        },
                    )
                }
            }
        }
        Text("انتخاب سرویس", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)

        if (providerId == "gemini") {
            Spacer(Modifier.height(10.dp)); Text("مدل Gemini", fontWeight = FontWeight.SemiBold)
            AiField(geminiModel, onGeminiModelChange, "Model ID (مثلاً gemini-3.8-flash)")
            Text("شناسه مدل قابل ویرایش است و بخشی از اطلاعات اتصال نیست.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }
        if (providerId == "groq") {
            Spacer(Modifier.height(10.dp)); Text("مدل Groq", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
            Box {
                GlassOutlinedButton(groqModel, { groqMenuExpanded.value = true }, Modifier.fillMaxWidth())
                DropdownMenu(expanded = groqMenuExpanded.value, onDismissRequest = { groqMenuExpanded.value = false }) { GROQ_MODELS.forEach { model -> DropdownMenuItem(text = { Text(model) }, onClick = { onGroqModelChange(model); groqMenuExpanded.value = false }) } }
            }
            Text("مدل انتخابی یک تنظیم معمولی است، نه اطلاعات محرمانه. هنگام ارسال تصویر، Groq به‌صورت خودکار از Qwen3.6-27B Vision استفاده می‌کند.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }
        if (providerId == "cloudflare") {
            Spacer(Modifier.height(10.dp)); Text("Cloudflare", fontWeight = FontWeight.SemiBold)
            Text("شناسه حساب", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
            GlassIdentifierField(cloudflareAccountId, onCloudflareAccountIdChange, "Account ID", supportingText = "شناسه حساب Cloudflare است؛ محرمانه نیست.")
            Spacer(Modifier.height(8.dp)); Text("مدل Cloudflare", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
            Box {
                GlassOutlinedButton(cloudflareModel, { cloudflareMenuExpanded.value = true }, Modifier.fillMaxWidth())
                DropdownMenu(expanded = cloudflareMenuExpanded.value, onDismissRequest = { cloudflareMenuExpanded.value = false }) { CLOUDFLARE_MODELS.forEach { model -> DropdownMenuItem(text = { Text(model) }, onClick = { onCloudflareModelChange(model); cloudflareMenuExpanded.value = false }) } }
            }
            Text("مدل انتخاب‌شده برای متن استفاده می‌شود؛ هنگام ارسال تصویر، Agent از مدل Vision مربوط به Cloudflare استفاده می‌کند.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("🔐  اعتبارنامه ${providerLabel(providerId)}", fontWeight = FontWeight.Bold)
                        Text(if (apiKey.isBlank()) "تنظیم نشده" else "روی همین دستگاه ذخیره شده", color = if (apiKey.isBlank()) GlassTokens.faint else GlassTokens.live, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("محرمانه", color = GlassTokens.accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                GlassCredentialField(value = apiKey, onValueChange = onApiKeyChange, label = if (providerId == "cloudflare") "API Token کلادفلر" else "کلید API ${providerLabel(providerId)}", supportingText = "این مقدار محرمانه است و فقط برای اتصال مستقیم ${providerLabel(providerId)} استفاده می‌شود.")
                Text(providerDescription(providerId), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        GlassButton("ذخیره تنظیمات", {
            vm.saveApiKey(apiKey)
            if (providerId == "gemini") vm.saveGeminiModel(geminiModel)
            if (providerId == "groq") vm.saveGroqModel(groqModel)
            if (providerId == "cloudflare") { vm.saveCloudflareModel(cloudflareModel); vm.saveCloudflareAccountId(cloudflareAccountId) }
            onDismiss()
        })
    }
}
