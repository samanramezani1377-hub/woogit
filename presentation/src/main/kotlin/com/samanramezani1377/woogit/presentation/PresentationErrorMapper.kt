package com.samanramezani1377.woogit.presentation

import com.samanramezani1377.woogit.core.domain.error.DomainError

object PresentationErrorMapper {
    // DEBUG ONLY: switch to false for the final release to hide technical/server details.
    private const val SHOW_TECHNICAL_DETAILS = true

    fun message(error: DomainError): String {
        val userMessage = when (error) {
            is DomainError.Authentication -> "احراز هویت فروشگاه ناموفق بود. کلیدهای اتصال و دسترسی کاربر را بررسی کنید."
            is DomainError.Permission -> "دسترسی لازم برای این عملیات در WooCommerce وجود ندارد."
            is DomainError.NotFound -> "مورد موردنظر در فروشگاه پیدا نشد."
            is DomainError.Validation -> "اطلاعات ارسالی برای فروشگاه معتبر نیست."
            is DomainError.Conflict -> "اطلاعات فروشگاه همزمان تغییر کرده است؛ دوباره تلاش کنید."
            is DomainError.RateLimited -> "تعداد درخواست‌ها زیاد است؛ کمی بعد دوباره تلاش کنید."
            is DomainError.Server -> "سرور فروشگاه هنگام پردازش درخواست با خطا مواجه شد."
            is DomainError.Network -> "ارتباط با فروشگاه برقرار نشد. اتصال اینترنت و آدرس فروشگاه را بررسی کنید."
            is DomainError.Unknown -> "خطای غیرمنتظره‌ای در ارتباط با فروشگاه رخ داد. دوباره تلاش کنید."
        }

        if (!SHOW_TECHNICAL_DETAILS) return userMessage

        val reason = when (error) {
            is DomainError.Authentication -> error.reason
            is DomainError.Permission -> error.reason
            is DomainError.NotFound -> error.id
            is DomainError.Validation -> error.reason
            is DomainError.Conflict -> error.reason
            is DomainError.RateLimited -> error.reason
            is DomainError.Server -> error.reason
            is DomainError.Network -> error.reason
            is DomainError.Unknown -> error.reason
        }.trim()

        return if (reason.isBlank()) userMessage else "$userMessage\n\nجزئیات فنی برای عیب‌یابی:\n$reason"
    }
}
