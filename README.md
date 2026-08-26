# WooGit

اپلیکیشن اندرویدی سریع و **Local-first / Offline-first** برای مدیریت تخصصی فروشگاه‌های WooCommerce؛ با تمرکز بر سه کار اصلی صاحب فروشگاه: **اعلان سفارش جدید، مشاهده و مدیریت سفارش‌ها، و افزودن/ویرایش سریع محصولات**.

WooGit قرار نیست یک پنل شلوغ یا نسخه‌ای دیگر از WordPress باشد. هدف آن این است که کارهای پرتکرار فروشگاه را با کمترین اصطکاک، داخل یک اپ native و سریع انجام دهد و تغییرات را ابتدا محلی ثبت و سپس با WooCommerce همگام کند.

## سه محور اصلی محصول

### 1. اعلان سفارش جدید
مهم‌ترین نقطه تماس WooGit. V1 بدون سرور اختصاصی WooGit از polling پس‌زمینه و WorkManager استفاده می‌کند؛ بنابراین اعلان «Push فوری» نیست و زمان اجرا تحت محدودیت‌های Android است. هدف محصولی V1 اطلاع‌رسانی در مقیاس دقیقه و حداکثر قابل‌قبول حدود یک ساعت است، با در نظر گرفتن محدودیت‌های سیستم‌عامل.

### 2. مشاهده و مدیریت سفارش
پس از اعلان، کاربر باید بتواند سفارش را سریع باز کند، اطلاعات مهم را در یک نگاه ببیند و عملیات مدیریتی موردنیاز را بدون مراجعه به پنل WooCommerce انجام دهد.

### 3. افزودن و اصلاح محصول
مسیر افزودن محصول از یک ثبت بسیار سریع تا ساخت کامل محصول ادامه پیدا می‌کند. Quick Add قابلیت جداگانه‌ای برای حذف امکانات نیست؛ Progressive Disclosure فقط برای سریع نگه داشتن مسیر اصلی استفاده می‌شود.

## فلسفه V1

V1 عمداً روی **ساخت پایه واقعی محصول** تمرکز دارد: Core پایدار، Local-first، Sync قابل اتکا، تجربه سریع و سه محور اصلی کامل.

قابلیت‌های آینده حذف نشده‌اند؛ Core باید از امروز برای پذیرش آن‌ها آماده باشد، اما **فعال‌سازی یا پیاده‌سازی واقعی آن‌ها تا نسخه مناسب به تعویق می‌افتد**. افزونه سبک WordPress برای Commit واقعی، Multi-Store فعال، Push Provider، AI و iOS بخشی از V1 نیستند مگر اینکه تصمیم صریح جدیدی ثبت شود.

## معماری در یک نگاه

- **Core-Out:** منطق تجاری و Sync مستقل از UI.
- **Kotlin Multiplatform:** مرز Core برای استفاده مجدد آینده.
- **Clean Architecture:** جداسازی Core از UI و جزئیات پلتفرم.
- **Local-first / Offline-first:** تغییر ابتدا محلی و Optimistic است.
- **SQLDelight:** دیتابیس محلی V1 با مرز KMP-compatible.
- **Pending Queue:** تغییر ناموفق تا Push موفق در دستگاه باقی می‌ماند.
- **WooCommerce REST API:** تنها سیستم remote در V1؛ **بدون سرور اختصاصی WooGit**.
- **WorkManager:** اجرای کارهای پس‌زمینه در Android.
- **Ktor + Coroutines/Flow:** شبکه و عملیات asynchronous.
- **Notification/Event boundary:** آماده برای Providerهای آینده بدون وابستگی V1 به Backend اختصاصی.
- **Multi-Store boundary:** Store Connection از ابتدا مستقل و قابل توسعه است، ولی محصول V1 تک‌فروشگاهی است.

## مدل Sync و نسخه داده

تغییرات کاربر ابتدا در دیتابیس محلی ثبت می‌شوند و UI فوراً نتیجه را نشان می‌دهد. سپس Sync Engine آن‌ها را به WooCommerce Push می‌کند. در حالت Offline، خطا یا Retry، تغییر در Pending Queue باقی می‌ماند.

در V1 برای تشخیص تغییر سمت فروشگاه از `date_modified_gmt` ووکامرس استفاده می‌شود. این مقدار **Commit واقعی نیست** و فقط نسخه تشخیصی سمت فروشگاه است. معماری Version Provider طوری طراحی می‌شود که در آینده بتوان آن را با Commit واقعی جایگزین یا تکمیل کرد.

برای mutationهای مبهم، مانند timeout بعد از ارسال درخواست، WooGit نباید کورکورانه همان mutation را تکرار کند؛ operation identity پایدار، persistence و reconciliation قبل از retry ناامن الزامی است.

## وضعیت فعلی

Foundation و لایه‌های Core/Data/Sync پروژه پیاده‌سازی شده‌اند و **E11 — Design System & UI** در حال تکمیل روی همین معماری است. Theme Foundation و Liquid Glass primitives اضافه شده‌اند و UI اصلی V1 اکنون از مسیر `E11ReleaseApp` و Composition Root موجود استفاده می‌کند. مرحله بعدی، E12 برای تکمیل و سخت‌گیری integration بین UI و Core است.

این وضعیت به معنی اجرای Test Suite یا اعلام Release نهایی نیست؛ تست‌ها طبق Scope فعلی این اجرای فنی خارج از معیار Done هستند.

## وضعیت مستندات و Source of Truth

README فقط **ورودی و نمای کلی پروژه** است و جزئیات در مستندات تخصصی نگهداری می‌شوند.

### Product & Architecture
- **[Product Vision](docs/PRODUCT_VISION.md)**
- **[Roadmap](docs/ROADMAP.md)**
- **[Architecture](docs/ARCHITECTURE.md)**
- **[V1 Architecture Decisions](docs/V1_ARCHITECTURE_DECISIONS.md)**
- **[V1 Pre-Build Gate](docs/V1_PREBUILD_GATE.md)**

### V1 Implementation Contracts
- **[V1 Design Specification](docs/V1_DESIGN_SPEC.md)**
- **[V1 Data Contracts](docs/V1_DATA_CONTRACTS.md)**
- **[V1 API Contract](docs/V1_API_CONTRACT.md)**
- **[V1 Schema](docs/V1_SCHEMA.md)**
- **[V1 Notification Specification](docs/V1_NOTIFICATION_SPEC.md)**
- **[V1 Error Catalog](docs/V1_ERROR_CATALOG.md)**
- **[V1 Permission & Security](docs/V1_PERMISSION_SECURITY.md)**
- **[V1 Traceability](docs/V1_TRACEABILITY.md)**

### Existing Detailed Documentation
- **[Orders & Products](docs/ORDERS_AND_PRODUCTS.md)**
- **[Connection & Sync](docs/CONNECTION_AND_SYNC.md)**
- **[Offline Queue & Push](docs/OFFLINE_QUEUE_AND_PUSH.md)**
- **[Commit Versioning](docs/COMMIT_VERSIONING.md)**
- **[Conflict Resolution](docs/CONFLICT_RESOLUTION.md)**
- **[Notifications & Events](docs/NOTIFICATIONS_AND_EVENTS.md)**
- **[Notification Specification](docs/NOTIFICATION_SPEC.md)**
- **[Notification Roadmap](docs/NOTIFICATION_ROADMAP.md)**
- **[Security & Auth](docs/SECURITY_AND_AUTH.md)**
- **[Security & Permissions](docs/SECURITY_AND_PERMISSIONS.md)**
- **[Local Data & Cache](docs/LOCAL_DATA_AND_CACHE.md)**
- **[Offline & Resilience](docs/OFFLINE_AND_RESILIENCE.md)**
- **[Search](docs/SEARCH.md)**
- **[Multi-Device](docs/MULTI_DEVICE.md)**
- **[Project Map](docs/PROJECT_MAP.md)**
- **[Decision Tracker](docs/DECISION_TRACKER.md)**
- **[Execution Plan](docs/EXECUTION_PLAN.md)**
- **[Execution Prompts](docs/EXECUTION_PROMPTS.md)**

## قانون مدیریت دانش پروژه

1. README محل نگهداری جزئیات کامل نیست.
2. هر تصمیم جدید باید در `DECISION_TRACKER.md` ثبت شود.
3. هر موضوع تخصصی باید در سند تخصصی خودش نگهداری شود و README فقط خلاصه و لینک آن را داشته باشد.
4. قابلیت آینده باید از نظر معماری آماده باشد، اما بدون تصمیم صریح نباید زودتر فعال شود.
5. هیچ قابلیت موجود بدون تصمیم صریح حذف نمی‌شود.
6. اسناد `docs/V1_*` قراردادهای اجرایی V1 هستند؛ در صورت تعارض، Decision Tracker و قراردادهای قفل‌شده باید بررسی و سپس تصمیم صریح ثبت شود.

## License

لایسنس نهایی پروژه می‌تواند تجاری/انحصاری یا BUSL-1.1 باشد. اگر در آینده افزونه همراه WordPress برای WordPress.org منتشر شود، آن بخش باید با الزامات GPLv2+ سازگار باشد.
