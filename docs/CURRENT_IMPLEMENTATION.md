# WooGit — Current Implementation Source of Truth

> این سند وضعیت واقعی پیاده‌سازی را از قراردادهای تاریخی، roadmap و ایده‌های آینده جدا می‌کند.

## اصل تقدم

در صورت تعارض بین مستندات و دستور صریح و جدید کاربر، **دستور جدید کاربر بر مستندات مقدم است**. پس از اجرای تصمیم جدید، سند مربوط باید به‌روزرسانی شود.

## وضعیت پایه فعلی

- شاخه مرجع: `main`
- این سند وضعیت implementation را توصیف می‌کند و نباید قابلیت‌هایی را که فقط در roadmap هستند به‌عنوان implemented معرفی کند.
- هیچ قابلیت موجود نباید صرفاً برای ساده‌سازی refactor یا سبز شدن CI حذف شود.

## معماری اجرایی

- Android UI با Jetpack Compose/Material 3 در لایه Presentation قرار دارد.
- Core/Domain از UI مستقل است.
- Data مسئول ارتباط با WooCommerce و mapping بین DTO و Domain است.
- WooCommerce REST API مرز remote اصلی V1 است.
- Local-first/Offline-first و Pending Queue بخشی از جهت معماری پروژه هستند.
- Sync و mutationها باید قابل retry، reconciliation و conflict handling باشند.

## Products

مسیر Product Editor باید بتواند محصول ساده و variable را مطابق قرارداد فعلی پروژه مدیریت کند.

### تصویر محصول — قرارداد اجرایی

وقتی کاربر از **انتخاب عکس از موبایل** استفاده می‌کند:

1. فایل انتخاب‌شده به Media Library فروشگاه WooCommerce/WordPress آپلود می‌شود.
2. پاسخ upload شامل `media.id` و `media.src` است.
3. هر دو مقدار در state مدل محصول نگهداری می‌شوند.
4. `media.id` باید به‌عنوان شناسه attachment تصویر محصول حفظ شود.
5. هنگام Create/Update Product، اگر ID موجود است، همان attachment باید در `product.images` ثبت شود.
6. URL صرفاً fallback برای حالتی است که ID در دسترس نباشد.
7. صرف آپلود موفق فایل به Media Library به معنی ثبت تصویر محصول نیست؛ association با Product باید در mutation محصول انجام شود.
8. Preview UI فقط نمایش وضعیت است و جایگزین ثبت `images` در request نیست.

### تغییر URL دستی

اگر کاربر URL تصویر را به‌صورت دستی تغییر دهد، نباید `imageId` قدیمی به‌اشتباه همراه URL جدید ارسال شود. ID و URL باید همیشه به یک تصویر اشاره کنند.

### Product Import / Export — وضعیت واقعی

- Settings مسیر Export همه محصولات و Import محصولات را دارد.
- Package فعلی `.woogit` یک ZIP با `manifest.json`، `products.json` و `media/` است.
- Format فعلی `woogit-products`، logical version برابر `1` و layout version برابر `1` است؛ هر دو temporary هستند.
- Export شامل Product، Category، Global Attribute/Term، Product Image و Variationهای Product Variable است.
- Export تصویر را از URL می‌خواند و در layout فعلی برای هر occurrence یک media entry می‌سازد؛ Export Media Dedup فعلاً وجود ندارد.
- Import قبل از mutation، format/version/layout، ZIP path، duplicate entry، size limit، تعداد Product/Image، Product ID/SKU و Media referenceها را validation می‌کند.
- Validation در سطح package است؛ وجود validation error باعث شروع نشدن mutation می‌شود.
- Media Import هر entry را مستقل پردازش می‌کند و failure یک Media نباید Import سایر Media/Productها را متوقف کند.
- Product و Variation failure به‌صورت مستقل مدیریت می‌شوند و reservationهای SKU در failure آزاد می‌شوند.
- Result آمار Product، Variation و Media را جدا نگه می‌دارد و `validationErrors` از `importErrors` جداست.
- Source Product/Variation ID شناسه مبدأ است و نباید مستقیماً به‌عنوان Destination ID استفاده شود. Variation جدید با Product مقصد ساخته می‌شود و Variation اضافه مقصد حذف نمی‌شود.
- Media reuse در Import با canonical URL، content hash و filename+content hash تلاش می‌شود؛ در صورت غیرقابل‌دسترسی بودن Media مقصد، Dedup قطعی نیست.
- Package credential یا API key فروشگاه را ذخیره نمی‌کند.
- جزئیات کامل و محدودیت‌های این قابلیت در `docs/PRODUCT_IMPORT_EXPORT.md` نگهداری می‌شود.

### مواردی که فعلاً implemented نیستند

- Export Media Dedup
- Idempotency تضمینی برای Product بدون SKU
- mapping table دائمی Source → Destination
- checksum مستقل برای تمام entryها
- migration واقعی schema/layout
- حذف خودکار Variationهای اضافه مقصد
- validation مستقل هر Product به‌جای رد package در validation failure

## Orders

صفحه سفارش برای عملیات پرتکرار سریع است و Edit Order برای جزئیات کامل استفاده می‌شود. عملیات mutation باید از مسیر Repository/Core عبور کند و مستقیماً از UI به API متصل نشود.

## Dashboard

مقادیر داشبورد باید از داده‌های واقعی WooCommerce و قراردادهای Domain تغذیه شوند. وضعیت loading نباید باعث نمایش موقت مقدار نادرست یا stale به‌عنوان مقدار قطعی شود.

## Money & Currency

مبلغ‌ها و واحد پول در صورت نمایش اطلاعات فروشگاه باید از configuration/response واقعی WooCommerce استفاده کنند و hard-code کردن currency یا فرض واحد پول مجاز نیست.

## Connection Status

عبارت‌هایی مانند «فروشگاه متصل است» فقط زمانی مجازند که نتیجه واقعی validation/connection در state موجود باشد؛ loading یا وجود صرف credential نباید به‌عنوان اتصال موفق نمایش داده شود.

## CI و تغییرات کد

هر تغییر کد باید:

1. کوچک و محدود به علت تغییر باشد.
2. از بازنویسی کامل فایل بدون ضرورت خودداری کند.
3. بعد از تغییر با diff بررسی شود.
4. حذف غیرمنتظره خطوط یا منطق موجود باعث توقف و اصلاح patch شود.
5. CI بررسی شود.
6. برای رفع compile error، قابلیت یا منطق موجود حذف نشود.

## وضعیت قابلیت‌ها

Roadmap و Design Spec می‌توانند قابلیت‌های آینده را تعریف کنند، اما وجود نام یک قابلیت در آن اسناد به معنی implemented بودن آن نیست. فقط کد فعلی و تست/CI موفق می‌تواند implementation را تأیید کند.
