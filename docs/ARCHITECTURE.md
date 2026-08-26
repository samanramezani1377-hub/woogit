# WooGit — Architecture Direction

## Core-Out

Core باید مستقل از UI باشد و Domain، Store Data، Sync، Queue و API contracts را مدیریت کند. UI فقط مصرف‌کننده Core است.

## KMP

هسته با Kotlin Multiplatform طراحی می‌شود تا منطق تجاری، شبکه، دیتابیس و Sync بعداً در iOS نیز قابل استفاده باشد.

## Offline-first / Git-like Sync

تغییرات ابتدا محلی و Optimistic هستند. سپس Push خودکار یا دستی آن‌ها را به WooCommerce می‌فرستد. صف تغییرات، retry، timeout و conflict handling باید قابل توسعه باشند.

## Data Versioning

اپ نسخه داده محلی را نگه می‌دارد و ابتدا نسخه سبک سرور را بررسی می‌کند؛ فقط در صورت تغییر Sync انجام می‌شود. مسیر دقیق می‌تواند date_modified_gmt یا endpoint اختصاصی WooGit باشد.

## Multi-Store Ready

Store Connection یک موجودیت مستقل است. Auth، داده، Sync، Queue، Notification و Event باید Store-scoped باشند، حتی اگر نسخه اول فقط یک Store داشته باشد.

## Future Modules

از ابتدا مرز معماری برای `ai`/`assistant`، Event/Tracking و Notification وجود داشته باشد. قابلیت آینده نباید باعث بازنویسی سه محور اصلی شود.

## Android

Jetpack Compose + Material 3، WorkManager، Coroutines/Flow و Ktor Client. انتخاب دیتابیس و DI باید با KMP سازگار باشد.

## Testing (V1 Mandatory)

سطح آزمایش V1 کامل است: Unit Test برای منطق Core (Sync، Versioning، Conflict Resolution، Pending Queue)، Integration Test برای REST API ووکامرس و دیتابیس محلی، و UI Test برای جریان‌های سه محور اصلی. تست شبکه ناپایدار/کند الزامی است و باید قطعی حین تغییر، Retry/Timeout و Idempotency عملیات Push را پوشش دهد.
