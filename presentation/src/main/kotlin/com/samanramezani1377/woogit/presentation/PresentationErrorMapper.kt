package com.samanramezani1377.woogit.presentation

import com.samanramezani1377.woogit.core.domain.error.DomainError

object PresentationErrorMapper {
    fun message(error: DomainError): String = when (error) {
        is DomainError.Authentication -> "احراز هویت فروشگاه ناموفق بود. کلیدهای اتصال را بررسی کنید."
        is DomainError.Permission -> "دسترسی لازم برای این عملیات در WooCommerce وجود ندارد."
        is DomainError.NotFound -> "مورد موردنظر پیدا نشد."
        is DomainError.Validation -> "اطلاعات واردشده معتبر نیست."
        is DomainError.Conflict -> "اطلاعات فروشگاه همزمان تغییر کرده است؛ تعارض را بررسی کنید."
        is DomainError.RateLimited -> "تعداد درخواست‌ها زیاد است؛ کمی بعد دوباره تلاش کنید."
        is DomainError.Server -> "سرور فروشگاه موقتاً در دسترس نیست."
        is DomainError.Network -> "اتصال شبکه برقرار نیست؛ داده‌های ذخیره‌شده در صورت وجود نمایش داده می‌شوند."
        is DomainError.Unknown -> "خطای غیرمنتظره‌ای رخ داد. دوباره تلاش کنید."
    }
}
