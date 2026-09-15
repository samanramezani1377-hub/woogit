package com.samanramezani1377.woogit.presentation.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassTextField
import com.samanramezani1377.woogit.presentation.GlassTokens
import org.json.JSONArray
import org.json.JSONObject

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

private fun markdownLine(line: String): String {
    Regex("^\\s*[-*+]\\s+(.*)$").find(line)?.let { return "• ${it.groupValues[1]}" }
    Regex("^\\s*(\\d+)[.)]\\s+(.*)$").find(line)?.let { return "${it.groupValues[1]}. ${it.groupValues[2]}" }
    Regex("^\\s*>\\s?(.*)$").find(line)?.let { return "│ ${it.groupValues[1]}" }
    return line
}

private fun markdownAnnotated(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val matches = sequenceOf(
        Regex("\\*\\*(.+?)\\*\\*").findAll(text).map { it to SpanStyle(fontWeight = FontWeight.Bold) },
        Regex("~~(.+?)~~").findAll(text).map { it to SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) },
        Regex("`([^`]+)`").findAll(text).map { it to SpanStyle(fontFamily = FontFamily.Monospace) },
        Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").findAll(text).map { it to SpanStyle(fontWeight = FontWeight.SemiBold) },
    ).flatMap { it.asIterable() }.sortedBy { it.first.range.first }
    var cursor = 0
    for ((match, style) in matches) {
        if (match.range.first < cursor) continue
        builder.append(text.substring(cursor, match.range.first))
        builder.pushStyle(style)
        builder.append(match.groupValues[1])
        builder.pop()
        cursor = match.range.last + 1
    }
    builder.append(text.substring(cursor))
    return builder.toAnnotatedString()
}

@Composable
private fun CodeBlock(code: String) {
    val context = LocalContext.current
    var copied by remember(code) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .07f)).padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { copyToClipboard(context, code); copied = true }) { Text(if (copied) "کپی شد" else "کپی") }
        }
        Text(code, color = GlassTokens.ink, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp))
    }
}

private fun fencedBody(line: String): String? {
    val fence = line.trimStart()
    if (!fence.startsWith("```") || !fence.drop(3).contains("```")) return null
    val payload = fence.drop(3)
    val close = payload.lastIndexOf("```")
    if (close < 0) return null
    val beforeClose = payload.substring(0, close)
    val firstNewline = beforeClose.indexOf('\n')
    val body = if (firstNewline >= 0) {
        val header = beforeClose.substring(0, firstNewline).trim()
        if (header.isBlank() || header.matches(Regex("^[A-Za-z0-9_+-]+$"))) beforeClose.substring(firstNewline + 1) else beforeClose
    } else {
        beforeClose
    }
    return body.trimEnd().takeIf { it.isNotBlank() }
}

@Composable
private fun MarkdownMessage(text: String) {
    val lines = text.replace("\r\n", "\n").replace("\r", "\n").split('\n')
    var inCode = false
    var code = StringBuilder()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            val fence = line.trimStart()
            if (!inCode) {
                fencedBody(line)?.let { body -> CodeBlock(body); return@forEach }
            }
            if (fence.startsWith("```") || fence.startsWith(":```") ) {
                if (inCode) {
                    CodeBlock(code.toString().trimEnd())
                    code = StringBuilder()
                    inCode = false
                } else {
                    inCode = true
                    val opening = if (fence.startsWith(":```")) fence.drop(4) else fence.drop(3)
                    val firstNewline = opening.indexOf('\n')
                    if (firstNewline >= 0) {
                        val header = opening.substring(0, firstNewline).trim()
                        if (!header.matches(Regex("^[A-Za-z0-9_+-]+$"))) code.append(opening).append('\n')
                        else code.append(opening.substring(firstNewline + 1)).append('\n')
                    } else if (opening.isNotBlank() && !opening.matches(Regex("^[A-Za-z0-9_+-]+$"))) {
                        code.append(opening).append('\n')
                    }
                }
                return@forEach
            }
            if (inCode) {
                code.append(line).append('\n')
                return@forEach
            }
            if (line.isBlank()) { Spacer(Modifier.height(4.dp)); return@forEach }
            if (Regex("^\\s*#{1,6}\\s+.+$").matches(line)) {
                val heading = line.trimStart(); val level = heading.takeWhile { it == '#' }.length; val content = heading.drop(level).trim()
                Text(markdownAnnotated(content), color = GlassTokens.ink, fontWeight = FontWeight.Bold, fontSize = when (level) { 1 -> 24.sp; 2 -> 21.sp; 3 -> 18.sp; 4 -> 17.sp; else -> 16.sp }, modifier = Modifier.padding(top = if (level <= 2) 5.dp else 2.dp)); return@forEach
            }
            if (Regex("^\\s*(-{3,}|_{3,}|\\*{3,})\\s*$").matches(line)) {
                Box(Modifier.fillMaxWidth().padding(vertical = 5.dp).height(1.dp).background(Color.Black.copy(alpha = .12f))); return@forEach
            }
            Text(markdownAnnotated(markdownLine(line)), color = GlassTokens.ink)
        }
        if (inCode && code.isNotEmpty()) CodeBlock(code.toString().trimEnd())
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("WooGit AI", text))
    Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
}

@Composable
internal fun HistoryItem(session: AiChatSession, onClick: () -> Unit) { GlassOutlinedButton(session.title, onClick, Modifier.fillMaxWidth()) }

@Composable
internal fun MessageBubble(message: AiMessage, onRetry: (() -> Unit)? = null) {
    val context = LocalContext.current
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Box(Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(20.dp)).background(if (user) GlassTokens.accent.copy(alpha = .12f) else Color.White.copy(alpha = .54f)).padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (user) "شما" else "WooGit AI", color = if (user) GlassTokens.accent else GlassTokens.ink, fontWeight = FontWeight.SemiBold)
                message.attachment?.let { attachment ->
                    val bitmap = remember(attachment) { BitmapFactory.decodeByteArray(attachment.bytes, 0, attachment.bytes.size)?.asImageBitmap() }
                    bitmap?.let { Image(it, contentDescription = attachment.name, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(14.dp))) }
                }
                if (message.content.isNotBlank()) MarkdownMessage(message.content)
                if (message.content.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { copyToClipboard(context, message.content) }) { Text("کپی") }
                        if (!user && onRetry != null) TextButton(onClick = onRetry) { Text("تلاش دوباره") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AiField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    GlassTextField(value = value, onValueChange = onValueChange, label = label, modifier = modifier, singleLine = singleLine, minLines = 1)
}