# WooGit Product Import / Export

> مرجع **implementation واقعی فعلی** است، نه roadmap. در اختلاف کد و سند، کد فعلی اولویت دارد و این سند باید sync شود.

## 1. Scope و Source of Truth

WooGit برای انتقال Productهای WooCommerce از فایل `.woogit` استفاده می‌کند. Package یک ZIP نسخه‌دار شامل Product، Variation، Category، Attribute/Term و Media است.

Source of Truthهای این قابلیت:

- `ProductTransferFormat.kt`
- `ProductTransferArchive.kt`
- `ProductTransferValidation.kt`
- `ProductTransferMedia.kt`
- `ProductTransferModels.kt`
- `ProductTransferMappers.kt`
- `ProductTransferRepositoryReader.kt`
- `RobustProductTransferService.kt`
- repositoryهای Product/Variation/Media/Category/Attribute
- Settings UI مربوط به Import/Export

Source ID و Destination ID مستقل‌اند و Source ID نباید بدون mapping/lookup به Destination mutation تبدیل شود.

## 2. `.woogit` Format

Layout فعلی v1:

```text
.woogit
├── manifest.json
├── products.json
└── media/
    ├── p-<product-id>-<index>.<ext>
    └── v-<variation-id>.<ext>
```

قرارداد فعلی:

- format: `woogit-products`
- format version: `1`
- layout version: `1`
- نسخه‌های فعلی در کد temporary هستند.
- Media extensionهای مجاز: `jpg`, `jpeg`, `png`, `webp`, `gif`, `svg`.
- pathهای absolute، `..` و backslash مجاز نیستند.
- `manifest.json` اطلاعات format/version/layout، source store و countها را دارد.
- `products.json` داده Product/Variation و referenceهای انتقال را دارد.
- `media/` فایل واقعی تصاویر را دارد.

## 3. Export

Pipeline واقعی Export:

1. Resolve فروشگاه.
2. Read Productها از repository.
3. enforce سقف `10,000` Product.
4. جمع‌آوری Category و Global Attribute/Termهای لازم.
5. Read Variable Product و Variationها.
6. دریافت Media URLها.
7. نوشتن Media entryها.
8. ساخت Image referenceها (`id`/`url`/`name`/`alt`/file reference).
9. تولید `manifest.json` و `products.json`.
10. ساخت ZIP `.woogit`.

Limits:

- Product: `10,000`
- هر Entry: `50 MiB`
- `products.json`: `100 MiB`
- کل Package: `1 GiB`

### Export Media Dedup

**در Export تضمین نشده است.** تصویر مشترک ممکن است چند بار با pathهای متفاوت وارد package شود. Dedup/reuse اصلی در Import انجام می‌شود.

Failure در ساخت داده ضروری یا دریافت Media نباید package ناقص را به‌عنوان Export موفق تحویل دهد.

## 4. Import Pipeline

```text
Read ZIP
  ↓
Validate package
  ↓
Resolve destination Media
  ↓
Read destination Products
  ↓
Resolve Categories / Global Attributes / Terms
  ↓
Match Product
  ↓
Create / Update Product
  ↓
Match + Create / Update Variations
  ↓
Aggregate Result
```

Package-level validation قبل از mutation انجام می‌شود.

## 5. Validation

Validation فعلی شامل:

- وجود `manifest.json` و `products.json`
- format/version/layout معتبر
- ZIP entry معتبر
- path traversal، absolute path و backslash protection
- duplicate ZIP entry
- package/entry/`products.json` limits
- تطبیق countهای manifest با محتوا
- Product limit
- Product ID/name/type/status
- ProductType/ProductStatus
- Variation attribute validity
- duplicate Product ID
- duplicate SKU داخل package
- وجود Media referenceهای لازم
- Global Attribute/Term validity
- duplicate Termهای یک Attribute

Validation **package-level** است. اگر validation error وجود داشته باشد mutation شروع نمی‌شود و خطا در `validationErrors` می‌آید. این با mutation failure فرق دارد.

## 6. Product Mapping

Destination state برای lookupهای Create/Update خوانده می‌شود. Source Product ID هویت مبدأ است و Destination ID باید از mapping/lookup مقصد به‌دست آید.

**Idempotency کامل بدون SKU تضمین نشده است.** Import مجدد package بدون SKU الزاماً همان Product مقصد را پیدا نمی‌کند.

## 7. Variation Mapping

Variation شامل Source ID، SKU، Attribute combination و Media reference است.

قواعد فعلی:

- Source Variation ID مستقیماً Destination Variation ID نیست.
- Variation جدید با `savedProduct.id` به **Product مقصد** متصل می‌شود.
- same-store می‌تواند Source Variation ID را برای lookup بررسی کند.
- cross-store از content-based matching استفاده می‌کند.
- canonical attribute combination باید مستقل از ترتیب Attributeها باشد.
- mapping database دائمی Source → Destination وجود ندارد.
- Variation اضافه مقصد حذف نمی‌شود.

ترتیب معماری موردنظر برای تطبیق: **mapping → SKU → canonical attribute combination**. جزئیات lookup باید همیشه با `ProductTransferMappers` و Service هماهنگ باشد.

## 8. SKU Reservation

SKUهای موجود مقصد و SKUهای رزروشده برای جلوگیری از collision در همان Import مدیریت می‌شوند.

```text
reserve
  ↓
mutation
  ├── success → retained
  └── failure / exception → released
```

Reservation database lock دائمی نیست. SKU خالی identity مبتنی بر SKU نمی‌سازد و idempotency بدون SKU تضمین نشده است.

## 9. Partial Failure

Isolation باید در هر سه سطح حفظ شود:

- شکست یک Product، Productهای بعدی را متوقف نمی‌کند.
- شکست یک Variation، Variationهای بعدی را متوقف نمی‌کند.
- شکست یک Media، Mediaهای دیگر یا Product loop را متوقف نمی‌کند.
- exception کلی Media به outcome failure تبدیل می‌شود تا Import ادامه یابد.
- Product موفقی که Variation آن fail شده همچنان Product موفق است.

Import transaction سراسری و rollback کل package ندارد؛ رفتار فعلی **partial-success** است.

## 10. Result و Statistics

Counterها فقط بر اساس operation واقعی ثبت می‌شوند.

### Product

- `created`: Create موفق
- `updated`: Update موفق
- `failed`: شکست mutation خود Product
- `drafted`: ایجاد موفق در مسیر Draft

### Variation

- `variationsCreated`
- `variationsUpdated`
- `variationsFailed`

### Media

- `imagesUploaded`: Upload موفق
- `imagesReused`: reuse واقعی مقصد
- `imagesFailed`: failure واقعی Media
- `imagesUnused`: Mediaهای unused طبق semantics فعلی

### Errors

- `validationErrors`: failure قبل از mutation
- `importErrors`: failure هنگام Import/mutation

Failure Variation/Media نباید Product موفق را failed کند.

## 11. Media Import / Reuse

Matching فعلی به‌صورت کلی:

1. canonical URL + content hash مقصد
2. Media resolve‌شده قبلی در همان Import با content hash
3. filename + hash مقصد
4. Upload در صورت نبود match

Hash مقصد با دریافت محتوای URL محاسبه می‌شود. اگر Media مقصد قابل دریافت نباشد Dedup مطلق تضمین نمی‌شود و ممکن است Upload جدید انجام شود.

هر Media entry مستقل پردازش می‌شود و `uploaded`/`reused` فقط از outcome واقعی می‌آیند.

## 12. Category / Attribute / Term

- Category chain در مقصد resolve می‌شود.
- Global Attribute در same-store می‌تواند با ID شناسایی شود.
- cross-store از slug/name استفاده می‌کند.
- Term با name/slug مقصد resolve یا create می‌شود.
- mapping دائمی Source → Destination برای این موجودیت‌ها guarantee نشده است.

## 13. Data Coverage

Transfer model فقط **فیلدهای صراحتاً تعریف‌شده در مدل انتقال** را حمل می‌کند. انتقال تمام WooCommerce metadata یا تمام فیلدهای API تضمین نشده است.

هر فیلد جدید تا زمانی که در Transfer model، mapper، archive و mutation پشتیبانی نشود، supported محسوب نمی‌شود.

## 14. Security / Limits

- path traversal ممنوع
- absolute path ممنوع
- backslash ممنوع
- duplicate entry ممنوع
- package ≤ `1 GiB`
- entry ≤ `50 MiB`
- `products.json` ≤ `100 MiB`
- Product ≤ `10,000`

## 15. Explicit Non-Guarantees

فعلاً این موارد implemented/guaranteed نیستند:

- Idempotency کامل بدون SKU
- Export Media Dedup قطعی
- mapping database دائمی Source → Destination
- checksum کامل همه entryها
- migration خودکار نسخه‌های آینده format
- حذف Variationهای اضافه مقصد
- rollback تراکنشی کل Import

## 16. وضعیت فعلی

| قابلیت | وضعیت |
|---|---|
| `.woogit` ZIP format | 🟢 Implemented |
| Manifest / Products JSON | 🟢 Implemented |
| Product Create/Update | 🟢 Implemented |
| Variable Product / Variation | 🟢 Implemented |
| Product partial failure | 🟢 Implemented |
| Variation partial failure | 🟢 Implemented |
| Media partial failure | 🟢 Implemented |
| SKU reservation cleanup | 🟢 Implemented |
| Validation/mutation error separation | 🟢 Implemented |
| Product/Variation/Media statistics | 🟢 Implemented |
| Source/Destination Variation ID separation | 🟢 Implemented |
| Destination Product parent integrity | 🟢 Implemented |
| Preserve extra destination Variations | 🟢 Implemented |
| Media reuse on Import | 🟢 Implemented |
| Export Media Dedup | 🔴 Not guaranteed |
| Idempotency without SKU | 🔴 Not guaranteed |
| Permanent cross-store mapping | 🔴 Not implemented |
| Full transaction rollback | 🔴 Not implemented |
| Automatic future-format migration | 🔴 Not implemented |

## 17. Agent Maintenance Rules

هر Agentی که Import/Export را تغییر می‌دهد باید:

1. ابتدا implementation واقعی را بررسی کند.
2. Source/Destination ID را قاطی نکند.
3. Product/Variation/Media partial failure را حفظ کند.
4. SKU reservation را در success/failure/exception cleanup کند.
5. Result counterها را فقط از operation واقعی تولید کند.
6. validation و mutation errors را جدا نگه دارد.
7. با تغییر Format/Layout/Transfer model این سند را همان تغییر sync کند.
8. roadmap را implemented اعلام نکند.
9. same-store و cross-store mapping را جدا بررسی کند.
10. هر تغییر Media identity/reuse را با hash/URL/filename semantics تطبیق دهد.

این سند توضیح implementation است و جایگزین قراردادهای type-safe کد نیست.
