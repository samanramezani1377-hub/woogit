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
            value.contains("unsafe_destination") || value.contains("unresolvable") ->
                "آدرس فروشگاه قابل دسترسی یا معتبر نیست. DNS و آدرس HTTPS سایت را بررسی کنید."
            value.contains("secure_transport_unavailable") ->
                "ارتباط امن با فروشگاه از سمت Backend در دسترس نیست. لطفاً کمی بعد دوباره تلاش کنید."
            value.contains("timeout") || value.contains("timed out") ->
                "زمان پاسخ‌گویی فروشگاه یا Backend به پایان رسید. اتصال اینترنت، DNS و SSL را بررسی و دوباره تلاش کنید."
            value.contains("backend http 401") ->
                "احراز هویت درخواست به Backend ناموفق بود. اتصال و دسترسی حساب را بررسی کنید."
            value.contains("backend http 429") ->
                "تعداد درخواست‌های اتصال بیش از حد مجاز است. کمی بعد دوباره تلاش کنید."
            value.contains("backend http 5") ->
                "Backend WooGit با خطای سرور مواجه شد. کمی بعد دوباره تلاش کنید."
            else ->
                "ارتباط با فروشگاه برقرار نشد. اتصال اینترنت و آدرس فروشگاه را بررسی کنید."
        }
    }
}
