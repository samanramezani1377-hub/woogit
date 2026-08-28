package com.samanramezani1377.woogit.presentation

import com.samanramezani1377.woogit.core.domain.error.DomainError

/** Single presentation boundary for user-facing errors. Technical details are kept out of UI. */
object PresentationErrorMapper {
    fun message(error: DomainError): String = when (error) {
        is DomainError.Authentication -> "احراز هویت فروشگاه ناموفق بود. کلیدهای اتصال و دسترسی کاربر را بررسی کنید."
        is DomainError.Permission -> "حساب متصل اجازه انجام این عملیات را ندارد. نقش کاربر و دسترسی‌های WordPress را بررسی کنید."
        is DomainError.Validation -> "اطلاعات ارسالی برای فروشگاه معتبر نیست. مشخصات درخواست را بررسی و دوباره تلاش کنید."
        is DomainError.NotFound -> "اطلاعات موردنظر در فروشگاه پیدا نشد."
        is DomainError.Conflict -> "اطلاعات با وضعیت فعلی فروشگاه تداخل دارد. دوباره تلاش کنید."
        is DomainError.RateLimited -> "تعداد درخواست‌ها بیش از حد مجاز است. کمی بعد دوباره تلاش کنید."
        is DomainError.Server -> "سرور فروشگاه با مشکل مواجه شده است. کمی بعد دوباره تلاش کنید."
        is DomainError.Network -> if (error.reason.contains("timeout", ignoreCase = true) || error.reason.contains("timed out", ignoreCase = true)) {
            "زمان پاسخ‌گویی فروشگاه به پایان رسید. دوباره تلاش کنید."
        } else {
            "ارتباط با فروشگاه برقرار نشد. اتصال اینترنت و آدرس فروشگاه را بررسی کنید."
        }
        is DomainError.Unknown -> "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید."
    }
}
