package com.samanramezani1377.woogit.presentation

import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.debug.TechnicalErrorReporter

/** Maps domain failures to safe user messages and records the technical context for debugging. */
object PresentationErrorMapper {
    fun message(error: DomainError): String {
        val userMessage = when (error) {
            is DomainError.Authentication -> "احراز هویت فروشگاه ناموفق بود. کلیدهای اتصال و دسترسی کاربر را بررسی کنید."
            is DomainError.Permission -> "حساب متصل اجازه انجام این عملیات را ندارد. نقش کاربر و دسترسی‌های WordPress را بررسی کنید."
            is DomainError.Validation -> "اطلاعات ارسالی برای فروشگاه معتبر نیست. مشخصات درخواست را بررسی و دوباره تلاش کنید."
            is DomainError.NotFound -> "اطلاعات موردنظر در فروشگاه پیدا نشد."
            is DomainError.Conflict -> "اطلاعات با وضعیت فعلی فروشگاه تداخل دارد. دوباره تلاش کنید."
            is DomainError.RateLimited -> "تعداد درخواست‌ها بیش از حد مجاز است. کمی بعد دوباره تلاش کنید."
            is DomainError.Server -> "سرور فروشگاه با مشکل مواجه شده است. کمی بعد دوباره تلاش کنید."
            is DomainError.Network -> detailedNetworkMessage(error.reason)
            is DomainError.Unknown -> "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید."
        }

        TechnicalErrorReporter.reportHandled(
            feature = "Presentation",
            location = "PresentationErrorMapper.message",
            operation = "Map DomainError to user message",
            userMessage = userMessage,
            technicalMessage = error.toString(),
            type = "DOMAIN_ERROR",
        )
        return userMessage
    }

    private fun detailedNetworkMessage(reason: String): String {
        val value = reason.lowercase()
        return when {
            value.contains("wordpress_unreachable") ->
                "اتصال به WordPress فروشگاه برقرار نشد. دسترسی اینترنتی، DNS و SSL سایت را بررسی کنید."
            value.contains("wordpress_auth_failed") ->
                "احراز هویت WordPress فروشگاه ناموفق بود. نام کاربری و Application Password را بررسی کنید."
            value.contains("woocommerce_unreachable") ->
                "اتصال به WooCommerce فروشگاه برقرار نشد. REST API، DNS و SSL سایت را بررسی کنید."
            value.contains("woocommerce_auth_failed") ->
                "احراز هویت WooCommerce ناموفق بود. Consumer Key و Consumer Secret را بررسی کنید."
            value.contains("site_verification_failed") ->
                "اعتبارسنجی فروشگاه ناموفق بود. مشخصات WordPress و WooCommerce و دسترسی REST API را بررسی کنید."
            value.contains("unknownhost") || value.contains("unknown host") || value.contains("name or service not known") || value.contains("nodename nor servname") ->
                "نام دامنه Backend یا فروشگاه پیدا نشد. آدرس دامنه و DNS را بررسی کنید."
            value.contains("sslhandshake") || value.contains("ssl handshake") || value.contains("certificate") || value.contains("certpath") ->
                "ارتباط امن HTTPS برقرار نشد. گواهی SSL و تنظیمات HTTPS دامنه را بررسی کنید."
            value.contains("connectexception") || value.contains("connection refused") || value.contains("failed to connect") ->
                "اتصال به Backend یا فروشگاه برقرار نشد. در دسترس بودن سرور و پورت HTTPS را بررسی کنید."
            value.contains("sockettimeout") || value.contains("timeout") || value.contains("timed out") ->
                "زمان پاسخ‌گویی Backend یا فروشگاه به پایان رسید. اتصال، DNS و SSL را بررسی و دوباره تلاش کنید."
            value.contains("unresolved address") || value.contains("unresolvable") ->
                "آدرس Backend یا فروشگاه قابل resolve نیست. دامنه و DNS را بررسی کنید."
            value.contains("unsafe_destination") ->
                "آدرس فروشگاه امن یا معتبر نیست. از آدرس HTTPS معتبر استفاده کنید."
            value.contains("secure_transport_unavailable") ->
                "ارتباط امن با فروشگاه از سمت Backend در دسترس نیست. لطفاً کمی بعد دوباره تلاش کنید."
            value.contains("backend http 401") ->
                "احراز هویت درخواست به Backend ناموفق بود. اتصال و دسترسی حساب را بررسی کنید."
            value.contains("backend http 429") ->
                "تعداد درخواست‌های اتصال بیش از حد مجاز است. کمی بعد دوباره تلاش کنید."
            value.contains("backend http 4") ->
                "درخواست اتصال به Backend پذیرفته نشد. اطلاعات اتصال و نسخه App را بررسی کنید."
            value.contains("backend http 5") ->
                "Backend WooGit با خطای سرور مواجه شد. کمی بعد دوباره تلاش کنید."
            else ->
                "ارتباط با Backend یا فروشگاه برقرار نشد. اتصال اینترنت، آدرس و دسترسی HTTPS را بررسی کنید."
        }
    }
}
