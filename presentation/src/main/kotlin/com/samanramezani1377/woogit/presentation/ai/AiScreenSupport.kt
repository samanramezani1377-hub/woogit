package com.samanramezani1377.woogit.presentation.ai

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassTokens
import org.json.JSONObject
import org.json.JSONArray

internal val AI_PROVIDERS = listOf("gemini", "openrouter", "deepseek", "groq", "cloudflare")
internal val GROQ_MODELS = listOf("openai/gpt-oss-20b", "openai/gpt-oss-120b", "qwen/qwen3.6-27b", "qwen/qwen3.8-27b", "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "minimaxai/minimax-m2.7")
internal val CLOUDFLARE_MODELS = listOf("@cf/zai-org/glm-4.7-flash", "@cf/google/gemma-4-26b-a4b-it")

internal fun providerLabel(id: String) = when (id) { "gemini" -> "Gemini"; "deepseek" -> "DeepSeek"; "groq" -> "Groq"; "cloudflare" -> "Cloudflare"; else -> "OpenRouter" }
internal fun providerModelLabel(id: String, geminiModel: String, groqModel: String, cloudflareModel: String) = when (id) { "gemini" -> geminiModel; "deepseek" -> "deepseek-v4-flash"; "groq" -> groqModel; "cloudflare" -> cloudflareModel; else -> "openrouter/free" }
internal fun providerDescription(id: String) = when (id) { "gemini" -> "اتصال مستقیم به Google Gemini API؛ مدل انتخاب‌شده با tool calling استفاده می‌شود."; "deepseek" -> "اتصال مستقیم به api.deepseek.com؛ Backend جداگانه لازم نیست."; "groq" -> "اتصال مستقیم به Groq API؛ مدل انتخاب‌شده برای متن و Qwen3.6-27B برای ورودی تصویر استفاده می‌شود."; "cloudflare" -> "اتصال مستقیم به Workers AI REST API؛ Account ID و API Token لازم است."; else -> "اتصال مستقیم به OpenRouter؛ روتر openrouter/free مدل مناسب را انتخاب می‌کند." }

internal fun toolDisplayName(name: String?): String = when (name) {
    "products_list" -> "فهرست محصولات"
    "products_get" -> "دریافت محصول"
    "products_get_image" -> "دریافت تصویر محصول"
    "products_image_add" -> "افزودن تصویر محصول"
    "products_image_set_primary" -> "تغییر تصویر اصلی محصول"
    "products_image_remove" -> "حذف تصویر محصول"
    "products_create" -> "ایجاد محصول"
    "products_update" -> "ویرایش محصول"
    "products_delete" -> "حذف محصول"
    "orders_list" -> "فهرست سفارش‌ها"
    "orders_get" -> "دریافت سفارش"
    "orders_update_status" -> "تغییر وضعیت سفارش"
    "memory_read" -> "خواندن حافظه کاری"
    "memory_write" -> "ثبت یادداشت در حافظه"
    "memory_update" -> "ویرایش یادداشت حافظه"
    "memory_delete" -> "حذف یادداشت از حافظه"
    null, "" -> "عملیات WooGit"
    else -> "اجرای عملیات WooGit"
}

internal fun formatToolArguments(raw: String): String = runCatching {
    val json = JSONObject(raw)
    val labels = mapOf("id" to "شناسه", "productId" to "شناسه محصول", "orderId" to "شناسه سفارش", "imageId" to "شناسه تصویر", "name" to "نام", "sku" to "کد کالا", "description" to "توضیحات", "price" to "قیمت", "regularPrice" to "قیمت اصلی", "salePrice" to "قیمت فروش", "status" to "وضعیت", "type" to "نوع", "page" to "صفحه", "perPage" to "تعداد در صفحه", "content" to "محتوا", "title" to "عنوان", "stockStatus" to "وضعیت موجودی", "stockQuantity" to "موجودی", "categoryId" to "شناسه دسته‌بندی", "categories" to "دسته‌بندی‌ها")
    buildString { json.keys().forEach { key -> val value = json.opt(key); if (length > 0) append("\n"); append(labels[key] ?: key); append(": "); append(if (value is JSONArray || value is JSONObject) value.toString() else value.toString()) } }
}.getOrElse { raw }

@Composable
internal fun HistoryItem(session: AiChatSession, onClick: () -> Unit) { GlassOutlinedButton(session.title, onClick, Modifier.fillMaxWidth()) }

@Composable
internal fun MessageBubble(message: AiMessage) {
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Box(Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(20.dp)).background(if (user) GlassTokens.accent.copy(alpha = .12f) else Color.White.copy(alpha = .54f)).padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (user) "شما" else "WooGit AI", color = if (user) GlassTokens.accent else GlassTokens.ink, fontWeight = FontWeight.SemiBold)
                message.attachment?.let { attachment ->
                    val bitmap = remember(attachment) { BitmapFactory.decodeByteArray(attachment.bytes, 0, attachment.bytes.size)?.asImageBitmap() }
                    bitmap?.let { Image(it, contentDescription = attachment.name, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(14.dp))) }
                }
                if (message.content.isNotBlank()) Text(message.content, color = GlassTokens.ink)
            }
        }
    }
}

@Composable
internal fun AiField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    GlassTextField(value = value, onValueChange = onValueChange, label = label, modifier = modifier, singleLine = singleLine, minLines = 1)
}
