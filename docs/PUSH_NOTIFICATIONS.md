# WooGit — Push Notifications

## تصمیم V1

اعلان سفارش جدید باید حتی زمانی که WooGit کاملاً بسته است به کاربر برسد.

همچنین اگر اینترنت دستگاه در لحظه‌ی ایجاد سفارش قطع باشد، رویداد نباید صرفاً به‌دلیل offline بودن دستگاه از دست برود. پس از برقراری اتصال، سیستم باید امکان دریافت/تحویل اعلان را داشته باشد، با رعایت deduplication.

## مسیر موردنظر

```text
WooCommerce Order Created
        ↓
WooGit Event / Delivery Layer
        ↓
Push Delivery
        ↓
Device
        ↓
Notification
        ↓
Deep Link → Order Details
```

## Offline Device

Push delivery باید طوری طراحی شود که eventهای مهم قابل تحویل مجدد باشند. اگر دستگاه در زمان مناسب online نباشد، پس از بازگشت اتصال باید event/notification از مسیر قابل اتکا دریافت شود یا از Sync/Server Event Log بازیابی شود.

این قابلیت نباید به باز بودن برنامه وابسته باشد.

## Reliability

- Event ID یکتا برای هر سفارش/رویداد
- جلوگیری از اعلان تکراری در Retry
- ثبت وضعیت delivery در صورت نیاز
- امکان recovery از Event/Sync state
- Deep Link مستقیم به سفارش

## Performance

Push Notification باید مستقل از مسیر عادی UI و Sync باشد و دریافت آن نباید نیازمند اجرای کامل برنامه یا دانلود داده‌های غیرضروری باشد. اطلاعات اعلان همان اطلاعات ضروری تعریف‌شده در `NOTIFICATION_SPEC.md` است.

## Security

Push payload نباید اطلاعات حساس غیرضروری داشته باشد. جزئیات کامل سفارش پس از باز شدن امن برنامه/Sync دریافت می‌شود و notification فقط اطلاعات لازم برای اطلاع‌رسانی سریع را حمل می‌کند.
