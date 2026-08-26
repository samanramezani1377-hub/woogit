# WooGit — Architecture Direction

## V1 remote boundary

نسخه اول **بدون سرور اختصاصی WooGit** ساخته می‌شود. اپ مستقیماً به REST API فروشگاه WooCommerce متصل می‌شود. در V1 هیچ WooGit backend، cloud database، push relay، websocket server یا افزونه اجباری همراه فروشگاه وجود ندارد.

## Core-Out

Core باید مستقل از UI باشد و Domain، Store Data، Sync، Queue و API contracts را مدیریت کند. UI فقط مصرف‌کننده Core است.

## KMP

هسته با Kotlin Multiplatform طراحی می‌شود تا منطق تجاری، شبکه، دیتابیس و Sync بعداً در iOS نیز قابل استفاده باشد.

## Local Database

V1 از **SQLDelight** برای دیتابیس محلی استفاده می‌کند. انتخاب به دلیل KMP compatibility، schema ownership، transaction/migration support و قابل‌تست بودن انجام شده است. لایه Data باید به abstractionهای داخلی پروژه وابسته باشد و implementation دیتابیس از Domain جدا بماند.

## Offline-first / Git-like Sync

تغییرات ابتدا محلی و Optimistic هستند. سپس Push خودکار یا دستی آن‌ها را به WooCommerce می‌فرستد. صف تغییرات، retry، timeout، idempotency/reconciliation و conflict handling باید پایدار و قابل توسعه باشند.

## Data Versioning

اپ نسخه داده محلی را نگه می‌دارد و ابتدا نسخه سبک سرور را بررسی می‌کند؛ فقط در صورت تغییر Sync انجام می‌شود. مسیر V1 استفاده از `date_modified_gmt` است؛ endpoint اختصاصی WooGit یا Commit واقعی به آینده موکول می‌شود.

## Multi-Store Ready

Store Connection یک موجودیت مستقل است. Auth، داده، Sync، Queue، Notification و Event باید Store-scoped باشند، حتی اگر نسخه اول فقط یک Store داشته باشد.

## Notifications

V1 از server push استفاده نمی‌کند. New-order detection با WorkManager و polling ووکامرس انجام می‌شود و notification روی خود دستگاه ساخته می‌شود. معماری Notification باید بعداً امکان جایگزینی detector با Push واقعی را بدون بازطراحی UI/Core فراهم کند.

## Future Modules

از ابتدا مرز معماری برای `ai`/`assistant`، Event/Tracking و Notification وجود داشته باشد. قابلیت آینده نباید باعث بازنویسی سه محور اصلی شود.

## Android

Jetpack Compose + Material 3، WorkManager، Coroutines/Flow و Ktor Client. DI و database باید با مرزهای KMP سازگار باشند.

## Testing (V1 Mandatory)

سطح آزمایش V1 کامل است: Unit Test برای منطق Core (Sync، Versioning، Conflict Resolution، Pending Queue)، Integration Test برای REST API ووکامرس و دیتابیس محلی، و UI Test برای جریان‌های سه محور اصلی. تست شبکه ناپایدار/کند الزامی است و باید قطعی حین تغییر، Retry/Timeout، idempotency و reconciliation عملیات Push را پوشش دهد.
