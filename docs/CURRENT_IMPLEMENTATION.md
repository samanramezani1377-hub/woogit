# WooGit — Current Implementation Source of Truth

> این سند وضعیت **واقعی implementation فعلی** را از roadmap، design idea و قابلیت‌های آینده جدا می‌کند. در صورت تعارض با دستور صریح و جدید کاربر، دستور جدید مقدم است و بعد از اجرای آن این سند باید با کد sync شود.

## 1. قواعد مرجع

- شاخه مرجع: `main`.
- فقط کد فعلی و نتیجه تست/CI می‌تواند implementation را تأیید کند.
- وجود یک قابلیت در roadmap یا design spec به معنی implemented بودن آن نیست.
- هیچ قابلیت موجودی صرفاً برای ساده‌سازی refactor یا سبز شدن CI نباید حذف شود.
- مستندات باید محدودیت‌ها و موارد not implemented را صریحاً اعلام کنند.

## 2. معماری اجرایی

- Android UI با Jetpack Compose/Material 3 در Presentation است.
- Core/Domain از UI مستقل است.
- Data مسئول ارتباط با WooCommerce و mapping DTO/Domain است.
- WooCommerce REST API مرز اصلی remote در V1 است.
- Local-first/Offline-first و Pending Queue بخشی از جهت معماری هستند.
- mutation و sync باید تا حد ممکن قابل retry، reconciliation و conflict handling باشند.

## 3. Product Editor — تصویر محصول

وقتی کاربر تصویر را از موبایل انتخاب می‌کند:

1. فایل به Media Library فروشگاه WooCommerce/WordPress upload می‌شود.
2. پاسخ upload شامل `media.id` و `media.src` است.
3. هر دو مقدار در state مدل محصول نگهداری می‌شوند.
4. `media.id` شناسه attachment تصویر محصول است.
5. هنگام Create/Update Product، اگر ID موجود است، همان attachment باید در `product.images` ثبت شود.
6. URL فقط fallback است.
7. upload موفق Media Library به‌تنهایی به معنی association موفق با Product نیست.
8. Preview UI جایگزین ثبت `images` در mutation نیست.

اگر URL دستی تغییر کند، `imageId` قدیمی نباید همراه URL جدید ارسال شود.

## 4. Product Import / Export — وضعیت واقعی

جزئیات کامل این قابلیت در `docs/PRODUCT_IMPORT_EXPORT.md` نگهداری می‌شود. خلاصه implementation فعلی:

### Package

- فرمت فایل: `.woogit`.
- Container: ZIP.
- Format: `woogit-products`.
- Logical version: `1`.
- Layout version: `1`.
- هر دو version فعلاً `TEMPORARY` هستند.
- اجزای اصلی: `manifest.json`، `products.json` و `media/`.
- Layout مسیرها توسط `ProductTransferFormat` کنترل می‌شود.

### Export

- Productهای موجود repository را export می‌کند.
- سقف Product فعلی `10,000` است.
- Productهای Variable همراه Variationها export می‌شوند.
- Category و Global Attribute/Termهای مورد استفاده در package قرار می‌گیرند.
- تصاویر Product و Variation از URL مبدأ دریافت و در package embed می‌شوند.
- سقف package `1 GiB` و سقف هر Entry `50 MiB` است.
- Export Media Dedup فعلاً **وجود ندارد**.
- Export failure نباید به‌عنوان package موفق/کامل گزارش شود.

### Import validation

قبل از mutation، package-level validation انجام می‌شود، از جمله:

- manifest/products وجود داشته باشند.
- format/version/layout پشتیبانی شوند.
- Entryهای ZIP مجاز باشند.
- path traversal و duplicate entry رد شوند.
- size/count limitها رعایت شوند.
- تعداد Product/Image با manifest سازگار باشد.
- Product ID و SKUهای متعارض داخل package بررسی شوند.
- Product type/status و Variation attributes معتبر باشند.
- Media referenceها وجود داشته باشند.
- Global Attribute/Term و duplicate Termهای داخلی بررسی شوند.

وجود validation error باعث شروع mutation نمی‌شود و در `validationErrors` گزارش می‌شود.

### Product / Variation matching

- Source Product/Variation ID صرفاً identity مبدأ است.
- Source ID نباید مستقیماً Destination ID شود.
- Variation جدید با Product مقصد ساخته می‌شود.
- Variation اضافه مقصد حذف نمی‌شود.
- Mapping table دائمی Source → Destination فعلاً وجود ندارد.
- Idempotency تضمینی برای Product بدون SKU فعلاً وجود ندارد.
- Variation matching فعلی بر پایه lookupهای موجود same-store و content-based lookup در cross-store است؛ یک mapping engine مستقل با قرارداد عمومی `mapping → SKU → canonical attributes` فعلاً مستند/implemented نشده است.

### SKU reservation

- SKUهای مقصد برای جلوگیری از collision در Import در نظر گرفته می‌شوند.
- Reservation برای entity جدید lifecycle موقت دارد.
- بعد از mutation موفق reservation باقی می‌ماند.
- بعد از failure یا exception reservation باید آزاد شود.
- Reservation database lock دائمی نیست.

### Partial failure

بعد از عبور از package validation:

- failure یک Product نباید Productهای دیگر را متوقف کند.
- failure یک Variation نباید Variationهای دیگر را متوقف کند.
- failure یک Media نباید Media/Productهای دیگر را متوقف کند.
- Product موفق نباید به‌خاطر failure بعدی Variation/Media به‌اشتباه `failed` شمرده شود.

### Result

Result باید outcome واقعی mutation را نشان دهد و شامل آمار مستقل Product/Variation/Media و taxonomy باشد، از جمله:

- Product: `created`, `updated`, `failed`, `drafted`, `skuChanged`.
- Variation: `variationsCreated`, `variationsUpdated`, `variationsFailed`.
- Media: `imagesUploaded`, `imagesReused`, `imagesFailed`, `imagesUnused`.
- Taxonomy: Category / Attribute / Term statistics.
- Errors: `validationErrors` و `importErrors` به‌صورت جدا.

Counter نباید صرفاً به‌خاطر ورود به یک مرحله یا exception بالادستی افزایش پیدا کند.

### Media

- Media destination برای reuse بررسی می‌شود.
- canonical URL و content hash در lookup فعلی استفاده می‌شوند.
- Media resolve شده در همان Import می‌تواند reuse شود.
- در صورت نبود match، Media upload می‌شود.
- `uploaded` و `reused` فقط از outcome واقعی Media می‌آیند.
- اگر Media مقصد قابل دریافت نباشد، content-hash dedup قطعی نیست.

## 5. Product Import / Export — موارد صراحتاً NOT IMPLEMENTED

موارد زیر نباید در مستندات یا UI به‌عنوان قابلیت کامل معرفی شوند:

- Export Media Dedup.
- Idempotency تضمینی Product بدون SKU.
- Persistent Source → Destination mapping table.
- checksum مستقل برای تمام Entryها.
- schema/layout migration واقعی.
- validation مستقل هر Product در برابر package-level rejection فعلی.
- حذف خودکار Variationهای اضافه مقصد.
- پوشش تضمینی تمام WooCommerce metadata/plugin-specific fields.

## 6. Product Import / Export — audit coverage

برای هر تغییر در Transfer Engine باید این زنجیره بررسی شود:

`Settings UI → Transfer Service → Validation → Format/Layout → Archive → Models/Serialization → Media → Category/Attribute/Term → Product → Variation → Result`

فایل‌های اصلی مرجع:

- `RobustProductTransferService`
- `ProductTransferFormat`
- `ProductTransferValidation`
- `ProductTransferMedia`
- Transfer models/serializers
- Product/Variation repositories
- MediaRepository
- Settings Import/Export UI
- `docs/PRODUCT_IMPORT_EXPORT.md`

## 7. Orders

صفحه سفارش برای عملیات پرتکرار سریع است و Edit Order برای جزئیات کامل استفاده می‌شود. mutation سفارش باید از Repository/Core عبور کند و UI نباید مستقیماً API را صدا بزند.

## 8. Dashboard

Dashboard باید از داده واقعی WooCommerce و قرارداد Domain تغذیه شود. Loading نباید مقدار stale یا موقت را به‌عنوان مقدار قطعی نمایش دهد.

## 9. Money & Currency

مبلغ و currency باید از configuration/response واقعی WooCommerce گرفته شود. hard-code کردن واحد پول مجاز نیست.

## 10. Connection Status

«فروشگاه متصل است» فقط با نتیجه واقعی connection/validation مجاز است. وجود credential یا loading به‌تنهایی connection موفق محسوب نمی‌شود.

## 11. Change discipline

برای هر تغییر کد:

1. تغییر باید کوچک و محدود به علت باشد.
2. بازنویسی کامل فایل بدون ضرورت ممنوع است.
3. diff باید بررسی شود.
4. حذف غیرمنتظره منطق موجود باید متوقف و اصلاح شود.
5. CI باید در چرخه توسعه بررسی شود.
6. compile error نباید با حذف قابلیت موجود حل شود.
7. بعد از تغییر behavior، documentation باید با implementation واقعی sync شود.

## 12. Documentation rule

این سند و `docs/PRODUCT_IMPORT_EXPORT.md` باید **وضعیت واقعی کد** را نشان دهند. هر موردی که هنوز implemented نیست باید صریحاً در بخش NOT IMPLEMENTED بماند و نباید به‌صورت guarantee نوشته شود.
