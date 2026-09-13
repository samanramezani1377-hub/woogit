package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

internal object AiAgentTools {
    fun definitions() = JSONArray().apply {
        put(tool("product_categories_list", "دسته‌بندی‌های محصولات فروشگاه را از مسیر WooGit پیدا کن. برای یافتن محصولات یک دسته، ابتدا با search نام دسته را پیدا کن و سپس id آن را به products_list به‌عنوان categoryId بده.", categoryListSchema()))
        put(tool("products_list", "فهرست محصولات با اطلاعات لازم برای تصمیم‌گیری؛ بدون بایت تصویر. برای جستجوی محصولات یک دسته از categoryId استفاده کن تا فقط همان دسته از فروشگاه درخواست شود. پاسخ pagination شامل endOfCollection و lastPage است و اگر endOfCollection=true بود دیگر صفحه بعدی را درخواست نکن.", listSchema()))
        put(tool("products_get", "جزئیات کامل محصول از مسیر WooGit؛ بدون ارسال بایت تصویر.", idSchema()))
        put(tool("products_get_image", "یک تصویر مشخص محصول را به‌صورت attachment واقعی از مسیر رسانه WooGit دریافت کن. URL تصویر به مدل داده نمی‌شود و برای تحلیل تصویری فقط attachment را استفاده کن.", imageSchema()))
        put(tool("products_image_add", "تصویر انتخاب‌شده و از قبل پیوست‌شده توسط کاربر را به محصول اضافه کن. تصویر از قبل در برنامه انتخاب شده است؛ هرگز نام فایل یا انتخاب دوباره تصویر را از کاربر نخواه. فقط شناسه محصول را مشخص کن؛ نیازمند تأیید.", imageAddSchema()))
        put(tool("products_image_set_primary", "یک تصویر موجود محصول را تصویر اصلی کن؛ نیازمند تأیید.", imageSchema()))
        put(tool("products_image_remove", "یک تصویر موجود محصول را از محصول جدا کن؛ نیازمند تأیید.", imageRemoveSchema()))
        put(tool("products_create", "ایجاد محصول؛ نیازمند تأیید.", genericProductSchema()))
        put(tool("products_update", "ویرایش محصول؛ نیازمند تأیید.", genericPatchSchema()))
        put(tool("products_delete", "حذف محصول؛ نیازمند تأیید.", idSchema()))
        put(tool("orders_list", "فهرست سفارش‌ها از مسیر WooGit.", listSchema()))
        put(tool("orders_get", "جزئیات سفارش از مسیر WooGit.", idSchema()))
        put(tool("orders_update_status", "تغییر وضعیت سفارش؛ نیازمند تأیید.", orderStatusSchema()))
        put(tool("memory_read", "حافظه کاری پایدار Agent را بخوان. فقط وقتی لازم است از یادداشت‌های بین Sessionها مطلع شوی از آن استفاده کن.", emptySchema()))
        put(tool("memory_write", "یک یادداشت مهم و واقعاً قابل استفاده در Sessionهای بعدی در حافظه کاری بنویس. تاریخچه گفتگو را کپی نکن.", memoryWriteSchema()))
        put(tool("memory_update", "یک یادداشت موجود حافظه کاری را با محتوای جدید بازنویسی کن.", memoryUpdateSchema()))
        put(tool("memory_delete", "یک یادداشت حافظه کاری را وقتی دیگر معتبر یا لازم نیست حذف کن.", memoryIdSchema()))
    }

    fun label(name: String) = when (name) {
        "product_categories_list" -> "در حال بررسی دسته‌بندی‌های محصولات"
        "products_list" -> "در حال بررسی فهرست محصولات"
        "products_get" -> "در حال دریافت محصول"
        "products_get_image" -> "در حال دریافت تصویر محصول"
        "products_image_add" -> "در حال آماده‌سازی افزودن تصویر محصول"
        "products_image_set_primary" -> "در حال آماده‌سازی تغییر تصویر اصلی"
        "products_image_remove" -> "در حال آماده‌سازی حذف تصویر محصول"
        "products_create" -> "در حال آماده‌سازی ایجاد محصول"
        "products_update" -> "در حال آماده‌سازی ویرایش محصول"
        "products_delete" -> "در حال آماده‌سازی حذف محصول"
        "orders_list" -> "در حال بررسی سفارش‌ها"
        "orders_get" -> "در حال دریافت سفارش"
        "orders_update_status" -> "در حال آماده‌سازی تغییر وضعیت سفارش"
        "memory_read" -> "در حال خواندن حافظه کاری"
        "memory_write" -> "در حال ثبت یادداشت در حافظه"
        "memory_update" -> "در حال بازنویسی حافظه"
        "memory_delete" -> "در حال حذف یادداشت از حافظه"
        else -> "در حال اجرای ابزار WooGit"
    }

    fun isWrite(name: String) = name.endsWith("_create") || name.endsWith("_update") || name.endsWith("_delete") || name == "orders_update_status" || name == "products_image_add" || name == "products_image_set_primary" || name == "products_image_remove"

    fun isMemory(name: String) = name == "memory_read" || name == "memory_write" || name == "memory_update" || name == "memory_delete"

    fun assistantToolCall(id: String, name: String, arguments: String, signature: String?) = JSONObject()
        .put("role", "assistant")
        .put("content", JSONObject.NULL)
        .put("tool_calls", JSONArray().put(JSONObject()
            .put("id", id)
            .put("type", "function")
            .put("function", JSONObject()
                .put("name", name)
                .put("arguments", arguments)
                .apply { signature?.let { put("thought_signature", it) } }
            )
        ))

    private fun idSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1))).put("required", JSONArray().put("id"))
    private fun categoryListSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("page", JSONObject().put("type", "integer").put("minimum", 1)).put("perPage", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 100)).put("search", JSONObject().put("type", "string")))
    private fun listSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("page", JSONObject().put("type", "integer").put("minimum", 1)).put("perPage", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 99)).put("search", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string")).put("categoryId", JSONObject().put("type", "integer").put("minimum", 1)))
    private fun imageSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("imageIndex", JSONObject().put("type", "integer").put("minimum", 0))).put("required", JSONArray().put("id"))
    private fun imageAddSchema() = idSchema()
    private fun imageRemoveSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("imageIndex", JSONObject().put("type", "integer").put("minimum", 0)).put("imageId", JSONObject().put("type", "string"))).put("required", JSONArray().put("id"))
    private fun orderStatusSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("status", JSONObject().put("type", "string"))).put("required", JSONArray().put("id").put("status"))
    private fun genericProductSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("sku", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("shortDescription", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string")).put("salePrice", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string")).put("stockQuantity", JSONObject().put("type", "number")).put("stockStatus", JSONObject().put("type", "string")).put("manageStock", JSONObject().put("type", "boolean")))
    private fun genericPatchSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer")).put("patch", genericProductSchema())).put("required", JSONArray().put("id").put("patch"))
    private fun emptySchema() = JSONObject().put("type", "object").put("properties", JSONObject())
    private fun memoryWriteSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("content", JSONObject().put("type", "string").put("minLength", 1))).put("required", JSONArray().put("content"))
    private fun memoryUpdateSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "string")).put("content", JSONObject().put("type", "string").put("minLength", 1))).put("required", JSONArray().put("id").put("content"))
    private fun memoryIdSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "string"))).put("required", JSONArray().put("id"))
    private fun tool(name: String, description: String, schema: JSONObject) = JSONObject().put("type", "function").put("function", JSONObject().put("name", name).put("description", description).put("parameters", schema))
}