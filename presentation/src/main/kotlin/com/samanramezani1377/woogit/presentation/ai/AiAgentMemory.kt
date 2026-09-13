package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONObject

internal class AiAgentMemory(context: Context, storeId: String) {
    private val store = AgentMemoryStore(context, storeId)

    fun contextText(): String {
        val saved = store.read()
        return "\n\nحافظه کاری پایدار Agent: یک حافظه کوچک برای یادداشت‌های کاری مهم وجود دارد که بین Sessionهای گفتگو باقی می‌ماند. این حافظه را فقط وقتی استفاده کن که اطلاعاتی واقعاً برای ادامه کار در Sessionهای بعدی ارزش نگهداری دارد؛ تاریخچه گفتگو را در آن کپی نکن. خودت مسئول تشخیص، نوشتن، بازنویسی و حذف یادداشت‌ها هستی. برای این کار از ابزارهای memory_read، memory_write، memory_update و memory_delete استفاده کن. اگر حافظه خالی است چیزی به‌صورت پیش‌فرض وجود ندارد.\n" +
            if (saved.length() == 0) "حافظه فعلاً خالی است." else "محتوای فعلی حافظه:\n${saved}"
    }

    fun executeTool(name: String, arguments: String): String = runCatching {
        val args = JSONObject(arguments)
        when (name) {
            "memory_read" -> store.read().toString()
            "memory_write" -> store.write(args.getString("content")).toString()
            "memory_update" -> store.write(args.getString("content"), args.getString("id")).toString()
            "memory_delete" -> JSONObject().put("deleted", store.delete(args.getString("id"))).toString()
            else -> JSONObject().put("error", "unknown memory tool").toString()
        }
    }.getOrElse { JSONObject().put("error", it.message ?: "memory operation failed").toString() }
}
