# WooGit — Notifications, Events & Future Tracking

## نسخه اول

Notification حیاتی نسخه اول: **سفارش جدید**. اعلان باید حتی با بسته بودن اپ دریافت شود، سریع و قابل اعتماد باشد و با لمس آن مستقیماً به سفارش برسد.

## معماری Event-driven

Notification Core از ابتدا باید عمومی و قابل توسعه باشد و مفاهیم زیر را داشته باشد:

- Event Type
- Source / Store
- Subscription / User Preference
- Timestamp
- Payload / Metadata
- Deep Link / Target

## رویدادهای آینده

معماری باید برای این رویدادها آماده باشد، بدون اینکه همه در نسخه اول فعال شوند:

- تغییر وضعیت سفارش
- پرداخت موفق/مهم
- لغو سفارش
- خطا یا تکمیل Sync
- رویدادهای موجودی و محصول
- ورود کاربر
- سایر Eventهای آینده

## Notification Center Live

در آینده یک Notification Center ثابت در اپ می‌تواند علاوه بر اعلان‌های سیستم، اطلاعات Live انتخاب‌شده توسط کاربر را نشان دهد. کاربر از Settings مشخص می‌کند چه Event/اطلاعاتی فعال باشد.

## User Tracker

یک Event/Tracking Layer مستقل برای دریافت Activityهای سایت در آینده در نظر گرفته می‌شود؛ از جمله Login و سایر رویدادهای قابل انتخاب، با رعایت تنظیمات و الزامات حریم خصوصی.

## Chatbot / Site Assistant

Core و ماژول `ai`/`assistant` باید در آینده بتوانند با Orders، Products، Notifications، Store Data و Event/Tracking کار کنند تا WooGit قابلیت تبدیل شدن به دستیار/Chatbot متصل به سایت را داشته باشد، بدون بازطراحی Core.

## Multi-Store

هر Event و Notification باید Store Source داشته باشد تا در آینده چند فروشگاه با هم اشتباه نشوند و Deep Link به فروشگاه و رکورد صحیح هدایت شود.
