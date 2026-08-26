# WooGit

اپلیکیشن اندرویدی سریع و **Local-first / Offline-first** برای مدیریت تخصصی فروشگاه‌های WooCommerce؛ با تمرکز بر سه کار اصلی صاحب فروشگاه: **اعلان سفارش جدید، مشاهده و مدیریت سفارش‌ها، و افزودن/ویرایش سریع محصولات**.

WooGit قرار نیست یک پنل شلوغ یا نسخه‌ای دیگر از WordPress باشد. هدف آن این است که کارهای پرتکرار فروشگاه را با کمترین اصطکاک، داخل یک اپ native و سریع انجام دهد و تغییرات را ابتدا محلی ثبت و سپس با WooCommerce همگام کند.

## سه محور اصلی محصول

### 1. اعلان سفارش جدید — فوری

مهم‌ترین نقطه تماس WooGit. کاربر باید حتی در حالت بسته بودن اپ، در مقیاس دقیقه از سفارش جدید مطلع شود؛ سقف تأخیر قابل‌قبول برای V1 حدود یک ساعت است.

### 2. مشاهده و مدیریت سفارش — روزانه/ساعتی

پس از اعلان، کاربر باید بتواند سفارش را سریع باز کند، اطلاعات مهم را در یک نگاه ببیند و عملیات مدیریتی موردنیاز را بدون مراجعه به پنل WooCommerce انجام دهد.

### 3. افزودن و اصلاح محصول — سریع و کامل

مسیر افزودن محصول از یک ثبت بسیار سریع تا ساخت کامل محصول ادامه پیدا می‌کند. Quick Add قابلیت جداگانه‌ای برای حذف امکانات نیست؛ Progressive Disclosure فقط برای سریع نگه داشتن مسیر اصلی استفاده می‌شود.

## فلسفه V1

V1 عمداً روی **ساخت پایه واقعی محصول** تمرکز دارد: یک Core پایدار، Local-first، Sync قابل اتکا، تجربه سریع و سه محور اصلی کامل.

قابلیت‌های آینده حذف نشده‌اند؛ Core باید از امروز برای پذیرش آن‌ها آماده باشد، اما **فعال‌سازی یا پیاده‌سازی واقعی آن‌ها تا نسخه مناسب به تعویق می‌افتد**. به‌خصوص افزونه سبک WordPress برای Commit واقعی، Multi-Store، Push Provider، AI و iOS بخشی از V1 نیستند مگر اینکه تصمیم صریح جدیدی ثبت شود.

## معماری در یک نگاه

- **Core-Out:** منطق تجاری و Sync مستقل از UI.
- **Kotlin Multiplatform:** آماده برای استفاده مجدد Core در iOS.
- **Clean Architecture:** جداسازی Core از UI و جزئیات پلتفرم.
- **Local-first / Offline-first:** تغییر ابتدا محلی و Optimistic است.
- **Pending Queue:** تغییر ناموفق تا Push موفق در دستگاه باقی می‌ماند.
- **WooCommerce REST API:** Backend اصلی V1؛ بدون سرور اختصاصی WooGit.
- **WorkManager:** اجرای کارهای پس‌زمینه در Android.
- **Ktor + Coroutines/Flow:** شبکه و عملیات asynchronous.
- **Notification/Event boundary:** آماده برای Providerها و Eventهای آینده بدون وابستگی V1 به Backend اختصاصی.
- **Multi-Store boundary:** Store Connection از ابتدا موجودیتی مستقل و قابل توسعه است، ولی محصول V1 تک‌فروشگاهی است.

## مدل Sync و نسخه داده

تغییرات کاربر ابتدا در دیتابیس محلی ثبت می‌شوند و UI فوراً نتیجه را نشان می‌دهد. سپس Sync Engine آن‌ها را به WooCommerce Push می‌کند. در حالت Offline، خطا یا Retry، تغییر در Pending Queue باقی می‌ماند.

در V1 برای تشخیص تغییر سمت فروشگاه از `date_modified_gmt` ووکامرس استفاده می‌شود. این مقدار **Commit واقعی نیست** و فقط نسخه تشخیصی سمت فروشگاه است. معماری Version Provider طوری طراحی می‌شود که در آینده بتوان آن را با Commit واقعی جایگزین یا تکمیل کرد.

## وضعیت مستندات و Source of Truth

README فقط **ورودی و نمای کلی پروژه** است و جزئیات تصمیم‌ها و نیازمندی‌ها در مستندات تخصصی نگهداری می‌شوند.

- **[Product Vision](docs/PRODUCT_VISION.md)** — چه چیزی می‌سازیم و چرا.
- **[Roadmap](docs/ROADMAP.md)** — چه چیزی در V1 فعال است و چه چیزی به آینده موکول شده.
- **[Architecture](docs/ARCHITECTURE.md)** — Core-Out، KMP و اصول معماری.
- **[V1 Architecture Decisions](docs/V1_ARCHITECTURE_DECISIONS.md)** — تصمیم‌های کلیدی معماری V1.
- **[Orders & Products](docs/ORDERS_AND_PRODUCTS.md)** — نیازمندی‌های عملیاتی سفارش و محصول.
- **[Connection & Sync](docs/CONNECTION_AND_SYNC.md)** — اتصال و راهبرد Sync.
- **[Offline Queue & Push](docs/OFFLINE_QUEUE_AND_PUSH.md)** — صف تغییرات و Push.
- **[Commit Versioning](docs/COMMIT_VERSIONING.md)** — مدل نسخه‌بندی داده.
- **[Conflict Resolution](docs/CONFLICT_RESOLUTION.md)** — مدیریت تعارض.
- **[Notifications & Events](docs/NOTIFICATIONS_AND_EVENTS.md)** — معماری Event و اعلان.
- **[Notification Specification](docs/NOTIFICATION_SPEC.md)** — مشخصات اعلان.
- **[Notification Roadmap](docs/NOTIFICATION_ROADMAP.md)** — مسیر توسعه اعلان‌ها.
- **[Security & Auth](docs/SECURITY_AND_AUTH.md)** — احراز هویت و اتصال فروشگاه.
- **[Security & Permissions](docs/SECURITY_AND_PERMISSIONS.md)** — سطح دسترسی و امنیت.
- **[Local Data & Cache](docs/LOCAL_DATA_AND_CACHE.md)** — داده محلی و Cache.
- **[Offline & Resilience](docs/OFFLINE_AND_RESILIENCE.md)** — رفتار در شرایط قطع و خطا.
- **[Search](docs/SEARCH.md)** — جستجو.
- **[Multi-Device](docs/MULTI_DEVICE.md)** — آمادگی معماری برای چند دستگاه.
- **[Project Map](docs/PROJECT_MAP.md)** — نقشه مستندات و ارتباط بخش‌ها.
- **[Decision Tracker](docs/DECISION_TRACKER.md)** — مرجع تصمیم‌های قطعی و پرسش‌های باز.

### قانون مدیریت دانش پروژه

1. README محل نگهداری جزئیات کامل نیست.
2. هر تصمیم جدید باید در `DECISION_TRACKER.md` ثبت شود.
3. هر موضوع تخصصی باید در سند تخصصی خودش نگهداری شود و README فقط خلاصه و لینک آن را داشته باشد.
4. قابلیت آینده باید از نظر معماری آماده باشد، اما بدون تصمیم صریح نباید زودتر فعال شود.
5. هیچ قابلیت موجود بدون تصمیم صریح حذف نمی‌شود.

## وضعیت فعلی

پروژه در مرحله **تعریف و تثبیت محصول/معماری پیش از کدنویسی** است. پیش از شروع پیاده‌سازی، جریان‌ها و صفحات V1 باید در سطح Hi-Fi و همراه با Design System اختصاصی Liquid Glass مشخص شوند.

برای ترتیب نسخه‌ها و مرز دقیق V1، مرجع اصلی `docs/ROADMAP.md` است.

## License

لایسنس نهایی پروژه می‌تواند تجاری/انحصاری یا BUSL-1.1 باشد. اگر در آینده افزونه همراه WordPress برای WordPress.org منتشر شود، آن بخش باید با الزامات GPLv2+ سازگار باشد.
