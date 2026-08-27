package com.samanramezani1377.woogit.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Converts WordPress/WooCommerce REST error responses into actionable Persian messages.
 * The raw response code is intentionally not shown to users, but is retained in the
 * message when an unknown error needs diagnosis.
 */
object WordPressErrorMapper {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun message(status: Int, body: String?): String {
        val code = parseCode(body)
        return when (code) {
            "rest_not_logged_in", "rest_authentication_required" ->
                "ورود یا احراز هویت وردپرس انجام نشده است. اتصال فروشگاه و کلیدهای دسترسی را بررسی کنید."
            "rest_cookie_invalid_nonce" ->
                "نشست امنیتی وردپرس معتبر نیست. اتصال فروشگاه را دوباره برقرار کنید."
            "rest_forbidden", "rest_cannot_create", "rest_cannot_edit", "rest_cannot_delete", "rest_cannot_read" ->
                "کاربر متصل به فروشگاه اجازه انجام این عملیات را ندارد. نقش کاربر و دسترسی‌های WordPress را بررسی کنید."
            "rest_upload_no_data" ->
                "وردپرس فایل تصویری دریافت نکرد. انتخاب فایل و ارسال داده تصویر را بررسی کنید."
            "rest_upload_file_too_big" ->
                "حجم تصویر بیشتر از محدودیت مجاز وردپرس یا سرور است. حجم تصویر را کاهش دهید یا محدودیت آپلود سرور را افزایش دهید."
            "rest_upload_file_type", "rest_upload_invalid_file" ->
                "نوع یا ساختار فایل تصویر توسط وردپرس قابل قبول نیست. تصویر را به JPG یا PNG معتبر تبدیل و دوباره امتحان کنید."
            "rest_upload_invalid_image", "rest_upload_invalid_dimensions" ->
                "وردپرس نتوانست تصویر را پردازش کند. فایل تصویر ممکن است خراب یا ابعاد آن نامعتبر باشد."
            "rest_upload_unknown_error", "rest_upload_sideload_error" ->
                "وردپرس هنگام ذخیره تصویر با خطای داخلی آپلود مواجه شد. وضعیت رسانه‌ها و خطاهای سرور وردپرس را بررسی کنید."
            "rest_invalid_param", "rest_missing_callback_param", "rest_missing_param" ->
                "اطلاعات ارسالی برای وردپرس کامل یا معتبر نیست. مشخصات فایل و پارامترهای درخواست را بررسی کنید."
            "rest_no_route" ->
                "مسیر API وردپرس پیدا نشد. فعال بودن REST API و آدرس صحیح فروشگاه را بررسی کنید."
            "rest_invalid_json" ->
                "پاسخ یا درخواست JSON وردپرس معتبر نیست. افزونه‌ها، پروکسی یا تنظیمات REST API را بررسی کنید."
            "woocommerce_rest_cannot_view" ->
                "کاربر متصل اجازه مشاهده این اطلاعات WooCommerce را ندارد. دسترسی‌های کاربر را بررسی کنید."
            "woocommerce_rest_cannot_create" ->
                "کاربر متصل اجازه ایجاد این مورد در WooCommerce را ندارد. مجوزهای کاربر و API را بررسی کنید."
            "woocommerce_rest_cannot_edit" ->
                "کاربر متصل اجازه ویرایش این مورد در WooCommerce را ندارد. مجوزهای کاربر و API را بررسی کنید."
            "woocommerce_rest_cannot_delete" ->
                "کاربر متصل اجازه حذف این مورد در WooCommerce را ندارد. مجوزهای کاربر و API را بررسی کنید."
            "woocommerce_rest_invalid_id", "woocommerce_rest_invalid_parameter" ->
                "شناسه یا پارامتر ارسالی به WooCommerce معتبر نیست. اطلاعات درخواست را بررسی کنید."
            else -> messageForStatus(status, code)
        }
    }

    private fun messageForStatus(status: Int, code: String?): String = when (status) {
        400 -> "وردپرس درخواست را نپذیرفت. مشخصات درخواست یا فایل ارسالی را بررسی کنید${suffix(code)}."
        401 -> "وردپرس احراز هویت این درخواست را نپذیرفت. کلیدهای اتصال و روش احراز هویت را بررسی کنید${suffix(code)}."
        403 -> "وردپرس دسترسی این درخواست را مسدود کرد. مجوز کاربر، REST API و افزونه‌های امنیتی را بررسی کنید${suffix(code)}."
        404 -> "مسیر یا منبع موردنظر در وردپرس پیدا نشد. آدرس REST API و مسیر فروشگاه را بررسی کنید${suffix(code)}."
        405 -> "روش ارسال این درخواست توسط وردپرس پشتیبانی یا مجاز نیست. endpoint و روش HTTP را بررسی کنید${suffix(code)}."
        413 -> "حجم درخواست از محدودیت سرور بیشتر است. محدودیت‌های upload_max_filesize، post_max_size و وب‌سرور را بررسی کنید${suffix(code)}."
        415 -> "نوع فایل یا Content-Type توسط سرور/وردپرس پذیرفته نشد. نوع تصویر را بررسی کنید${suffix(code)}."
        429 -> "تعداد درخواست‌ها بیش از حد مجاز شده است. کمی صبر کنید و دوباره تلاش کنید${suffix(code)}."
        in 500..599 -> "وردپرس یا سرور فروشگاه هنگام پردازش درخواست خطای داخلی داد. لاگ WordPress و سرور را بررسی کنید${suffix(code)}."
        else -> "وردپرس درخواست را با خطای HTTP $status رد کرد. تنظیمات REST API، دسترسی‌ها و پاسخ سرور را بررسی کنید${suffix(code)}."
    }

    private fun parseCode(body: String?): String? = runCatching {
        body?.takeIf { it.isNotBlank() }?.let {
            json.parseToJsonElement(it).jsonObject["code"]?.jsonPrimitive?.content
        }
    }.getOrNull()

    private fun suffix(code: String?): String = code?.let { " (کد خطا: $it)" } ?: ""
}
