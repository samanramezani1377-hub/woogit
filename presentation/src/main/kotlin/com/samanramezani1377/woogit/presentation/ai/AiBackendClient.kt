package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class AgentReply(val text: String = "", val confirmationToken: String? = null, val toolName: String? = null, val toolArguments: String? = null)

internal class AiBackendClient(
    context: Context,
    private val dependencies: V1PresentationDependencies? = null,
    private val storeId: StoreId? = null,
) {
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    private val pending = mutableMapOf<String, PendingAction>()

    var apiKey: String
        get() = prefs.getString("deepseek_api_key", "") ?: ""
        set(value) { prefs.edit().putString("deepseek_api_key", value.trim()).apply() }

    suspend fun agent(messages: List<Pair<String, String>>, confirmationToken: String? = null): AgentReply {
        val working = JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
        messages.forEach { (role, content) -> working.put(JSONObject().put("role", role).put("content", content)) }
        if (confirmationToken != null) {
            val action = pending.remove(confirmationToken) ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            working.put(assistantToolCall(action.callId, action.name, action.arguments))
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", execute(action.name, action.arguments)))
        }
        repeat(MAX_STEPS) {
            val response = request(working)
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: throw IllegalStateException("DeepSeek پاسخ معتبری برنگرداند.")
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) return AgentReply(text = message.optString("content"))
            working.put(message)
            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val name = fn.optString("name")
                val arguments = fn.optString("arguments", "{}")
                val callId = call.optString("id")
                if (name.isBlank() || callId.isBlank()) throw IllegalStateException("DeepSeek ابزار نامعتبر ارسال کرد.")
                if (isWriteTool(name)) {
                    val token = tokenFor(name, arguments)
                    pending[token] = PendingAction(name, arguments, callId)
                    return AgentReply(confirmationToken = token, toolName = name, toolArguments = arguments)
                }
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", execute(name, arguments)))
            }
        }
        throw IllegalStateException("Agent به حداکثر مراحل مجاز رسید.")
    }

    private suspend fun request(messages: JSONArray): JSONObject {
        if (apiKey.isBlank()) throw IllegalStateException("کلید API دیپ‌سیک تنظیم نشده است.")
        val body = JSONObject().put("model", "deepseek-v4-flash").put("messages", messages).put("thinking", JSONObject().put("type", "disabled")).put("stream", false).put("tools", toolDefinitions()).put("tool_choice", "auto")
        val connection = (URL("https://api.deepseek.com/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 120_000; doOutput = true
            setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("DeepSeek HTTP $status: ${text.take(400)}")
            JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun toolDefinitions() = JSONArray().apply {
        put(tool("products_list", "فهرست محصولات موجود در WooGit.", listSchema()))
        put(tool("products_get", "دریافت یک محصول از WooGit.", idSchema()))
        put(tool("products_create", "ایجاد محصول از مسیر رسمی WooGit؛ نیازمند تأیید کاربر.", JSONObject().apply { put("type", "object"); put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("sku", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string"))); put("required", JSONArray().put("name")); put("additionalProperties", false) }))
        put(tool("products_update", "به‌روزرسانی محصول فقط از مسیر رسمی WooGit؛ نیازمند تأیید کاربر.", productPatchSchema()))
        put(tool("products_delete", "حذف محصول از طریق WooGit؛ نیازمند تأیید کاربر.", idSchema()))
        put(tool("orders_list", "فهرست سفارش‌ها از طریق WooGit.", listSchema()))
        put(tool("orders_get", "دریافت سفارش از طریق WooGit.", idSchema()))
        put(tool("orders_update_status", "تغییر وضعیت سفارش از طریق WooGit؛ نیازمند تأیید کاربر.", JSONObject().apply { put("type", "object"); put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("status", JSONObject().put("type", "string").put("enum", JSONArray().apply { OrderStatus.values().forEach { put(it.name) } }))); put("required", JSONArray().put("id").put("status")); put("additionalProperties", false) }))
    }

    private suspend fun execute(name: String, raw: String): String {
        val deps = dependencies ?: throw IllegalStateException("وابستگی‌های WooGit برای Agent آماده نیستند.")
        val store = storeId ?: throw IllegalStateException("فروشگاه فعالی برای Agent وجود ندارد.")
        val a = JSONObject(raw)
        val result: CoreResult<*> = when (name) {
            "products_list" -> deps.getProducts(store, a.optInt("page", 1), a.optInt("perPage", 20), a.optString("search").takeIf { it.isNotBlank() })
            "products_get" -> deps.getProduct(store, EntityId(a.getLong("id").toString()))
            "products_create" -> deps.createProduct(store, Product(EntityId("new"), a.getString("name"), a.optString("sku").takeIf { it.isNotBlank() }, a.optString("description").takeIf { it.isNotBlank() }, null, ProductStatus.DRAFT, ProductType.SIMPLE, Pricing(a.optString("regularPrice").takeIf { it.isNotBlank() }, null, false), null, emptyList(), emptyList(), emptyList(), null))
            "products_update" -> updateProduct(deps, store, a)
            "products_delete" -> deps.deleteProduct(store, EntityId(a.getLong("id").toString()))
            "orders_list" -> deps.getOrders(store, a.optInt("page", 1), a.optInt("perPage", 20), a.optString("search").takeIf { it.isNotBlank() }, a.optString("status").takeIf { it.isNotBlank() })
            "orders_get" -> deps.getOrder(store, EntityId(a.getLong("id").toString()))
            "orders_update_status" -> updateOrderStatus(deps, store, a)
            else -> throw IllegalArgumentException("ابزار ناشناخته: $name")
        }
        return when (result) {
            is CoreResult.Success -> JSONObject().put("ok", true).put("data", stringify(result.value)).toString()
            is CoreResult.Failure -> JSONObject().put("ok", false).put("error", result.error.toString()).toString()
        }
    }

    private suspend fun updateProduct(deps: V1PresentationDependencies, store: StoreId, a: JSONObject): CoreResult<*> {
        val id = EntityId(a.getLong("id").toString())
        return when (val current = deps.getProduct(store, id)) {
            is CoreResult.Success -> { val p = current.value; val patch = a.getJSONObject("patch"); deps.updateProduct(store, id, p.copy(name = if (patch.has("name")) patch.getString("name") else p.name, sku = if (patch.has("sku")) patch.optString("sku").takeIf { it.isNotBlank() } else p.sku, description = if (patch.has("description")) patch.getString("description") else p.description, shortDescription = if (patch.has("shortDescription")) patch.getString("shortDescription") else p.shortDescription, pricing = p.pricing.copy(regular = if (patch.has("regularPrice")) patch.getString("regularPrice") else p.pricing.regular, sale = if (patch.has("salePrice")) patch.getString("salePrice") else p.pricing.sale, onSale = if (patch.has("salePrice")) patch.getString("salePrice").isNotBlank() else p.pricing.onSale))) }
            is CoreResult.Failure -> current
        }
    }

    private suspend fun updateOrderStatus(deps: V1PresentationDependencies, store: StoreId, a: JSONObject): CoreResult<*> {
        val id = EntityId(a.getLong("id").toString())
        return when (val current = deps.getOrder(store, id)) {
            is CoreResult.Success -> deps.updateOrder(store, id, current.value.copy(status = OrderStatus.valueOf(a.getString("status"))))
            is CoreResult.Failure -> current
        }
    }

    private fun stringify(v: Any?): Any = when (v) { null -> JSONObject.NULL; is Product -> productJson(v); is Order -> JSONObject().put("id", v.id.value).put("number", v.number).put("status", v.status.name).put("total", v.total ?: JSONObject.NULL).put("currency", v.currency ?: JSONObject.NULL); is List<*> -> JSONArray().apply { v.forEach { put(stringify(it)) } }; else -> v.toString() }
    private fun productJson(p: Product) = JSONObject().apply { put("id", p.id.value); put("name", p.name); put("sku", p.sku ?: JSONObject.NULL); put("status", p.status.name); put("type", p.type.name); put("regularPrice", p.pricing.regular ?: JSONObject.NULL); put("salePrice", p.pricing.sale ?: JSONObject.NULL); put("stockQuantity", p.stock?.quantity ?: JSONObject.NULL); put("stockStatus", p.stock?.status?.name ?: JSONObject.NULL) }
    private fun tokenFor(name: String, args: String) = sha256("$name:$args").take(32)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun isWriteTool(name: String) = name.endsWith("_create") || name.endsWith("_update") || name.endsWith("_delete") || name == "orders_update_status"
    private fun assistantToolCall(id: String, name: String, args: String) = JSONObject().put("role", "assistant").put("content", JSONObject.NULL).put("tool_calls", JSONArray().put(JSONObject().put("id", id).put("type", "function").put("function", JSONObject().put("name", name).put("arguments", args))))
    private fun tool(name: String, description: String, schema: JSONObject) = JSONObject().put("type", "function").put("function", JSONObject().put("name", name).put("description", description).put("parameters", schema))
    private fun idSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1))).put("required", JSONArray().put("id")).put("additionalProperties", false)
    private fun listSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("page", JSONObject().put("type", "integer").put("minimum", 1)).put("perPage", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 100)).put("search", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string"))).put("additionalProperties", false)
    private fun productPatchSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("patch", JSONObject().put("type", "object").put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("sku", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("shortDescription", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string")).put("salePrice", JSONObject().put("type", "string")).put("stockQuantity", JSONObject().put("type", "number")).put("stockStatus", JSONObject().put("type", "string")).put("additionalProperties", false))).put("required", JSONArray().put("id").put("patch")).put("additionalProperties", false)
    private data class PendingAction(val name: String, val arguments: String, val callId: String)
    companion object { private const val MAX_STEPS = 6; private const val SYSTEM_PROMPT = "تو Agent داخلی WooGit هستی. تمام اطلاعات و تغییرات فروشگاه باید فقط از ابزارهای WooGit استفاده کنند. هرگز API ووکامرس را مستقیم صدا نزن. عملیات تغییردهنده فقط پس از تأیید صریح کاربر اجرا می‌شوند. پاسخ نهایی کوتاه، دقیق و فارسی باشد." }
}
