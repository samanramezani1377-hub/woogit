package com.samanramezani1377.woogit.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps WordPress/WooCommerce REST errors to Persian messages suitable for users.
 *
 * DEBUG SWITCH:
 * Set SHOW_TECHNICAL_DETAILS to false for the final release. This is intentionally
 * a single-line change so production never exposes API/server diagnostics.
 */
object WordPressErrorMapper {
    private const val SHOW_TECHNICAL_DETAILS = true
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun message(status: Int, body: String?): String {
        val details = parse(body)
        val userMessage = when (details.code) {
            "rest_not_logged_in", "rest_authentication_required" ->
                "احراز هویت وردپرس انجام نشده است. اتصال فروشگاه و کلیدهای دسترسی را بررسی کنید."
            "rest_cookie_invalid_nonce" ->
                "نشست امنیتی وردپرس معتبر نیست. اتصال فروشگاه را دوباره برقرار کنید."
            "rest_forbidden", "rest_cannot_create", "rest_cannot_edit", "rest_cannot_delete", "rest_cannot_read" ->
                "حساب متصل اجازه انجام این عملیات را ندارد. نقش کاربر و دسترسی‌های WordPress را بررسی کنید."
            "rest_upload_no_data" ->
                "وردپرس هیچ فایل تصویری دریافت نکرد. فایل را دوباره انتخاب و ارسال کنید."
            "rest_upload_file_too_big" ->
                "حجم تصویر بیش از حد مجاز است. حجم تصویر را کاهش دهید یا محدودیت آپلود فروشگاه را بررسی کنید."
            "rest_upload_file_type", "rest_upload_invalid_file" ->
                "فرمت تصویر توسط وردپرس پذیرفته نشد. تصویر را به JPG یا PNG معتبر تبدیل و دوباره تلاش کنید."
            "rest_upload_invalid_image", "rest_upload_invalid_dimensions" ->
                "وردپرس نتوانست این تصویر را پردازش کند. ممکن است فایل خراب باشد یا ابعاد آن معتبر نباشد."
            "rest_upload_unknown_error", "rest_upload_sideload_error" ->
                "ذخیره تصویر در وردپرس ناموفق بود. رسانه‌ها و خطاهای سرور فروشگاه را بررسی کنید."
            "rest_invalid_param", "rest_missing_callback_param", "rest_missing_param" ->
                "اطلاعات ارسالی کامل یا معتبر نیست. مشخصات درخواست را بررسی و دوباره تلاش کنید."
            "rest_no_route" ->
                "مسیر REST API وردپرس پیدا نشد. آدرس فروشگاه و فعال بودن REST API را بررسی کنید."
            "rest_invalid_json" ->
                "داده ارسالی با قالب مورد انتظار وردپرس سازگار نیست. اطلاعات درخواست را بررسی کنید."
            "woocommerce_rest_cannot_view" ->
                "حساب متصل اجازه مشاهده این اطلاعات WooCommerce را ندارد. دسترسی‌های کاربر را بررسی کنید."
            "woocommerce_rest_cannot_create" ->
                "حساب متصل اجازه ایجاد این مورد در WooCommerce را ندارد. دسترسی کاربر و کلید API را بررسی کنید."
            "woocommerce_rest_cannot_edit" ->
                "حساب متصل اجازه ویرایش این مورد در WooCommerce را ندارد. دسترسی کاربر و کلید API را بررسی کنید."
            "woocommerce_rest_cannot_delete" ->
                "حساب متصل اجازه حذف این مورد در WooCommerce را ندارد. دسترسی کاربر و کلید API را بررسی کنید."
            "woocommerce_rest_invalid_id" ->
                "شناسه موردنظر در WooCommerce معتبر نیست. اطلاعات انتخاب‌شده را بررسی کنید."
            "woocommerce_rest_invalid_parameter" ->
                "یکی از اطلاعات ارسالی برای WooCommerce معتبر نیست. مشخصات محصول و پارامترها را بررسی کنید."
            else -> messageForStatus(status)
        }

        return if (SHOW_TECHNICAL_DETAILS) {
            buildString {
                append(userMessage)
                append("\n\nجزئیات فنی برای عیب‌یابی:\nHTTP: ")
                append(status)
                details.code?.let { append("\nکد وردپرس: ").append(it) }
                details.serverMessage?.let { append("\nپیام وردپرس: ").append(it) }
            }
        } else {
            userMessage
        }
    }

    private fun messageForStatus(status: Int): String = when (status) {
        400 -> "فروشگاه اطلاعات ارسالی را نپذیرفت. مشخصات درخواست یا محصول را بررسی کنید."
        401 -> "فروشگاه احراز هویت این درخواست را نپذیرفت. کلیدهای اتصال را بررسی کنید."
        403 -> "دسترسی این عملیات توسط فروشگاه یا سرور مسدود شد. مجوز کاربر و تنظیمات امنیتی را بررسی کنید."
        404 -> "مسیر یا مورد موردنظر در فروشگاه پیدا نشد. آدرس REST API را بررسی کنید."
        405 -> "این نوع درخواست توسط endpoint فروشگاه مجاز نیست. روش و مسیر API را بررسی کنید."
        413 -> "حجم درخواست از محدودیت سرور بیشتر است. محدودیت حجم آپلود سرور را بررسی کنید."
        415 -> "نوع فایل یا Content-Type توسط فروشگاه پذیرفته نشد. فرمت فایل را بررسی کنید."
        422 -> "فروشگاه اطلاعات ارسالی را از نظر اعتبار نپذیرفت. مقادیر واردشده را بررسی کنید."
        429 -> "تعداد درخواست‌ها بیش از حد مجاز شده است. کمی صبر کنید و دوباره تلاش کنید."
        in 500..599 -> "فروشگاه یا سرور هنگام پردازش درخواست با خطای داخلی مواجه شد. وضعیت سرور و WordPress را بررسی کنید."
        else -> "فروشگاه درخواست را نپذیرفت. اتصال، تنظیمات REST API و اطلاعات ارسالی را بررسی کنید."
    }

    private data class ParsedError(val code: String?, val serverMessage: String?)

    private fun parse(body: String?): ParsedError = runCatching {
        if (body.isNullOrBlank()) return ParsedError(null, null)
        val objectValue = json.parseToJsonElement(body).jsonObject
        ParsedError(
            code = objectValue["code"]?.jsonPrimitive?.content,
            serverMessage = objectValue["message"]?.jsonPrimitive?.content,
        )
    }.getOrDefault(ParsedError(null, null))
}
