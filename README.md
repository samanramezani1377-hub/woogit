# WooGit

اپلیکیشن اندرویدی سریع و **Local-first / Offline-first** برای مدیریت تخصصی فروشگاه‌های WooCommerce؛ با تمرکز بر سه کار اصلی صاحب فروشگاه: **اعلان سفارش جدید، مشاهده و مدیریت سفارش‌ها، و افزودن/ویرایش سریع محصولات**.

WooGit قرار نیست یک پنل شلوغ یا نسخه‌ای دیگر از WordPress باشد. هدف آن این است که کارهای پرتکرار فروشگاه را با کمترین اصطکاک، داخل یک اپ native و سریع انجام دهد و تغییرات را ابتدا محلی ثبت و سپس با WooCommerce همگام کند.

## سه محور اصلی محصول

### 1. اعلان سفارش جدید
V1 بدون سرور اختصاصی WooGit از polling پس‌زمینه و WorkManager استفاده می‌کند؛ بنابراین اعلان V1 Push فوری نیست و زمان اجرای آن تحت محدودیت‌های Android است.

### 2. مشاهده و مدیریت سفارش
کاربر باید بتواند سفارش را سریع باز کند، اطلاعات مهم را ببیند و عملیات مدیریتی موردنیاز را بدون مراجعه به پنل WooCommerce انجام دهد.

### 3. افزودن و اصلاح محصول
مسیر افزودن محصول یک جریان واحد از Quick Add تا ساخت کامل محصول است و از محصول ساده، محصول متغیر، Variation، Attributes، دسته‌بندی و رسانه پشتیبانی می‌کند.

## وضعیت پیاده‌سازی فعلی

این بخش وضعیت **کد موجود در شاخه اصلی** را توصیف می‌کند و از برنامه‌های آینده جداست.

### Dashboard
- داده‌های فروش از WooCommerce خوانده می‌شوند.
- مبلغ فروش بر اساس داده فروشگاه محاسبه می‌شود.
- واحد پول از تنظیمات/داده WooCommerce گرفته می‌شود و نباید به‌صورت ثابت در UI تعریف شود.
- سفارش‌های `cancelled` و `failed` نباید در مجموع فروش موفق حساب شوند.
- وضعیت اتصال/Sync باید بر اساس وضعیت واقعی لایه داده نمایش داده شود، نه صرفاً یک متن ثابت.

### Orders
- فهرست و جزئیات سفارش وجود دارد.
- تغییر وضعیت سفارش به WooCommerce متصل است.
- یادداشت سفارش و عملیات سفارش از مسیر Core/Data انجام می‌شوند.
- عملیات تغییرات باید با مدل Local-first و Pending Queue سازگار بمانند.

### Products
- فهرست محصولات و جزئیات/ویرایش محصول وجود دارد.
- افزودن و ویرایش محصول در یک Product Editor انجام می‌شود.
- محصول ساده و متغیر و Variationها در محدوده V1 پشتیبانی می‌شوند.
- Attributes سفارشی و سراسری در معماری محصول وجود دارند.
- Category picker و کنترل‌های وضعیت محصول در Editor وجود دارند.
- حذف محصول در مسیر mutation پروژه پشتیبانی می‌شود.

### Product Media — قرارداد مهم

انتخاب تصویر از موبایل فقط به معنی «آپلود فایل به Media Library» نیست.

رفتار مورد انتظار این است:

`Mobile image → WooCommerce/WordPress Media upload → دریافت Media ID + URL → نگهداری هر دو در Product Editor → ثبت همان Media به‌عنوان Product Image هنگام Create/Update Product`

بنابراین:

- `media.id` باید حفظ شود.
- `media.src` باید برای preview و fallback حفظ شود.
- هنگام ثبت محصول، اگر Media ID معتبر وجود دارد، همان ID باید در `product.images` به WooCommerce ارسال شود.
- URL به‌تنهایی نباید جای association با Media attachment را بگیرد وقتی ID موجود است.
- اگر کاربر تصویر انتخاب‌شده را عوض کرد، reference تصویر قبلی نباید به‌اشتباه روی محصول باقی بماند.
- preview UI فقط نمایش وضعیت است و به‌تنهایی به معنی ثبت تصویر روی محصول نیست.

## معماری در یک نگاه

- **Core-Out:** منطق تجاری و Sync مستقل از UI.
- **Kotlin Multiplatform:** مرز Core برای استفاده مجدد آینده.
- **Clean Architecture:** جداسازی Core از UI و جزئیات پلتفرم.
- **Local-first / Offline-first:** تغییر ابتدا محلی و Optimistic است.
- **SQLDelight:** دیتابیس محلی V1 با مرز KMP-compatible.
- **Pending Queue:** تغییر ناموفق تا Push موفق در دستگاه باقی می‌ماند.
- **WooCommerce REST API:** سیستم remote اصلی V1؛ بدون سرور اختصاصی WooGit.
- **WorkManager:** اجرای کارهای پس‌زمینه در Android.
- **Ktor + Coroutines/Flow:** شبکه و عملیات asynchronous.
- **Notification/Event boundary:** آماده برای Providerهای آینده بدون وابستگی V1 به Backend اختصاصی.
- **Multi-Store boundary:** Store Connection مستقل و قابل توسعه است، ولی فعال‌سازی محصول V1 تک‌فروشگاهی است.

## مدل Sync و نسخه داده

تغییرات کاربر ابتدا در دیتابیس محلی ثبت می‌شوند و UI فوراً نتیجه را نشان می‌دهد. سپس Sync Engine تغییر را به WooCommerce Push می‌کند. در حالت Offline، Retry یا خطای قابل‌بازیابی، تغییر در Pending Queue باقی می‌ماند.

در V1 برای تشخیص تغییر سمت فروشگاه از `date_modified_gmt` ووکامرس استفاده می‌شود. این مقدار **Commit واقعی نیست** و فقط نسخه تشخیصی سمت فروشگاه است.

برای mutationهای مبهم، مانند timeout بعد از ارسال درخواست، WooGit نباید کورکورانه همان mutation را تکرار کند؛ operation identity پایدار، persistence و reconciliation قبل از retry ناامن الزامی است.

## UI و Design System

UI اصلی V1 بر پایه Theme Foundation و Liquid Glass primitives پروژه است و Composition Root فعلی نقطه ورود UI را فراهم می‌کند. تغییرات visual باید تا حد امکان در Presentation/Theme محدود بمانند و نباید منطق Core/Data را بدون نیاز تغییر دهند.

## V1 Scope

V1 روی سه محور اصلی تمرکز دارد:

1. اعلان سفارش جدید
2. مشاهده و مدیریت سفارش
3. مدیریت محصولات

قابلیت‌های آینده مانند Push Provider واقعی، Multi-Store فعال، Commit واقعی با Companion Plugin/Backend، AI/Assistant و iOS نباید بدون تصمیم صریح وارد V1 شوند.

## قوانین قطعی توسعه

### 1. دستور کاربر بر مستندات مقدم است

مستندات پروژه قرارداد فنی و مرجع زمینه‌ای هستند، اما **دستور صریح و جدید صاحب پروژه بر متن مستندات مقدم است**. اگر دستور جدید با سندی تعارض داشت، دستور جدید مبناست و تصمیم پایدار باید در سند مربوط ثبت شود.

### 2. هیچ قابلیت موجود بدون تصمیم صریح حذف نمی‌شود

اصلاح، refactor یا رفع CI نباید باعث حذف ناخواسته منطق موجود شود. قبل از commit باید diff بررسی شود و حذف بزرگ یا غیرمرتبط متوقف شود.

### 3. تغییرات حداقلی

برای Bug Fix باید کوچک‌ترین تغییر ممکن انجام شود. بازنویسی کامل فایل یا جایگزین کردن یک فایل کامل برای اصلاح چند خط مجاز نیست مگر اینکه صریحاً لازم و بررسی شده باشد.

### 4. CI معیار صحت build است، نه مجوز حذف کد

اگر CI شکست خورد، علت باید در همان مسیر اصلاح شود. سبز کردن CI با حذف قابلیت، کاهش scope تست یا حذف منطق موجود مجاز نیست.

### 5. کنترل قبل از commit

هر تغییر کد باید حداقل این موارد را بررسی کند:

- diff واقعی commit
- فایل‌ها و خطوط حذف‌شده
- عدم تغییر ناخواسته در قابلیت‌های غیرمرتبط
- compile/test/lint در صورت امکان
- حفظ قراردادهای Core/Data/API

## مستندات و Source of Truth

README نمای کلی پروژه و قوانین توسعه را نگه می‌دارد. جزئیات فنی در اسناد تخصصی `docs/` نگهداری می‌شوند.

### Product & Architecture
- `docs/PRODUCT_VISION.md`
- `docs/ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/V1_ARCHITECTURE_DECISIONS.md`
- `docs/V1_PREBUILD_GATE.md`

### V1 Contracts
- `docs/V1_DESIGN_SPEC.md`
- `docs/V1_DATA_CONTRACTS.md`
- `docs/V1_API_CONTRACT.md`
- `docs/V1_SCHEMA.md`
- `docs/V1_NOTIFICATION_SPEC.md`
- `docs/V1_ERROR_CATALOG.md`
- `docs/V1_PERMISSION_SECURITY.md`
- `docs/V1_TRACEABILITY.md`

### Implementation
- `docs/ORDERS_AND_PRODUCTS.md`
- `docs/CONNECTION_AND_SYNC.md`
- `docs/OFFLINE_QUEUE_AND_PUSH.md`
- `docs/COMMIT_VERSIONING.md`
- `docs/CONFLICT_RESOLUTION.md`
- `docs/NOTIFICATIONS_AND_EVENTS.md`
- `docs/SECURITY_AND_AUTH.md`
- `docs/SECURITY_AND_PERMISSIONS.md`
- `docs/LOCAL_DATA_AND_CACHE.md`
- `docs/OFFLINE_AND_RESILIENCE.md`
- `docs/SEARCH.md`
- `docs/MULTI_DEVICE.md`
- `docs/PROJECT_MAP.md`
- `docs/DECISION_TRACKER.md`
- `docs/EXECUTION_PLAN.md`
- `docs/EXECUTION_PROMPTS.md`

## قانون مدیریت دانش

1. README محل جزئیات تخصصی کامل نیست.
2. تصمیم‌های جدید و پایدار باید در `docs/DECISION_TRACKER.md` ثبت شوند.
3. موضوعات تخصصی در سند تخصصی خودشان نگهداری می‌شوند.
4. قابلیت آینده بدون تصمیم صریح زودتر فعال نمی‌شود.
5. هیچ قابلیت موجود بدون تصمیم صریح حذف نمی‌شود.
6. در تعارض بین اسناد، قراردادها و Decision Tracker باید ابتدا تعارض حل شود.
7. **دستور صریح کاربر بر اسناد قبلی اولویت دارد.**
8. تغییرات مستندات باید با commit واضح و قابل ردیابی ثبت شوند.

## License

لایسنس نهایی پروژه می‌تواند تجاری/انحصاری یا BUSL-1.1 باشد. اگر در آینده افزونه همراه WordPress برای WordPress.org منتشر شود، آن بخش باید با الزامات GPLv2+ سازگار باشد.
