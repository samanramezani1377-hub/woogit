# WooGit Product Import / Export

> این سند **وضعیت واقعی کد فعلی** را مستند می‌کند؛ قرارداد ایده‌آل یا roadmap نیست. مرجع کد بررسی‌شده، پیاده‌سازی موجود در `main` است. هر قابلیت فقط در صورتی اینجا implemented محسوب می‌شود که در کد فعلی وجود داشته باشد.

## 1. Format contract

WooGit از فایل `.woogit` برای انتقال محصولات استفاده می‌کند. فایل یک ZIP نسخه‌دار است و در پیاده‌سازی فعلی شامل این اجزای منطقی است:

- `manifest.json` — فرمت، نسخه منطقی، نسخه layout، URL فروشگاه مبدأ و تعداد محصولات/تصاویر.
- `products.json` — Productها، Categoryها، Attributeها، Global Attribute/Termها، Image referenceها و Variationها.
- `media/` — فایل واقعی تصاویر Product و Variation.

مقادیر فعلی از `ProductTransferFormat.kt` گرفته می‌شوند:

- Format: `woogit-products`
- Format version: `1`
- Layout version: `1`
- هر دو نسخه در حال حاضر **TEMPORARY** هستند.

`ProductTransferFormat` مرجع مسیرهای ZIP است و نوشتن Entryهای Export از gateway آن عبور می‌کند. Import نیز مسیرها را با همان Layout اعتبارسنجی می‌کند. fileciteturn46file0L2-L6

### Layout فعلی v1

```text
.woogit
├── manifest.json
├── products.json
└── media/
    ├── p-<product-id>-<index>.<ext>
    └── v-<variation-id>.<ext>
```

فرمت‌های Media مجاز در Layout فعلی: `jpg`, `jpeg`, `png`, `webp`, `gif`, `svg` هستند. مسیرهای absolute، `..` و backslash رد می‌شوند. fileciteturn46file0L2-L6

## 2. Export — وضعیت واقعی

Export در حال حاضر:

1. فروشگاه را resolve می‌کند.
2. محصولات را از Product repository می‌خواند.
3. سقف فعلی Product برابر `10,000` است.
4. Categoryها و Global Attribute/Termهای استفاده‌شده را جمع‌آوری می‌کند.
5. Productهای Variable و Variationهایشان را Export می‌کند.
6. تصاویر Product و Variation را از URL مبدأ دریافت می‌کند.
7. برای هر تصویر یک فایل در `media/` می‌نویسد.
8. شناسه، URL، نام و alt تصویر را در `TransferImage` نگه می‌دارد.
9. `manifest.json` و `products.json` را در ZIP قرار می‌دهد.
10. حجم کل package حداکثر `1 GiB` و حجم هر Entry حداکثر `50 MiB` است.

### نکته مهم درباره Export Media

در implementation فعلی **Media Dedup در Export انجام نمی‌شود**. اگر یک تصویر چند بار در Product/Variationهای مختلف استفاده شود، ممکن است چند Entry مستقل با مسیرهای مختلف داخل package قرار گیرد. Dedup فعلی مربوط به مسیر Import است، نه Export.

Export در صورت شکست دریافت یک تصویر، package را موفق اعلام نمی‌کند و ساخت خروجی fail می‌شود؛ بنابراین خروجی ناقص به‌عنوان Export موفق برگردانده نمی‌شود. نوشتن ZIP نیز از Layout kernel عبور می‌کند. fileciteturn59file0L2-L6

## 3. Import — مراحل فعلی

Import به‌صورت کلی این pipeline را دارد:

```text
Read ZIP
  ↓
Validate package
  ↓
Resolve destination Media
  ↓
Read destination Products
  ↓
Resolve Categories / Global Attributes
  ↓
Match Product
  ↓
Create / Update Product
  ↓
Match + Create / Update Variations
  ↓
Produce result statistics
```

### Validation قبل از Mutation

قبل از mutation، موارد زیر بررسی می‌شوند:

- وجود `manifest.json`
- وجود `products.json`
- format صحیح
- format version پشتیبانی‌شده
- layout version پشتیبانی‌شده
- مجاز بودن ZIP entryها
- جلوگیری از path traversal
- عدم وجود Entry تکراری
- سقف package برابر `1 GiB`
- سقف `products.json` برابر `100 MiB`
- سقف Entry برابر `50 MiB`
- تطبیق تعداد Productهای manifest با `products.json`
- تطبیق تعداد تصاویر manifest با Media entryها
- سقف Product برابر `10,000`
- شناسه و نام و نوع و status معتبر Product
- معتبر بودن `ProductType` و `ProductStatus`
- اعتبار Attributeهای Variation
- duplicate Product ID داخل package
- duplicate SKU داخل package
- وجود Media referenceهای Product/Variation در package
- اعتبار Global Attribute و Termها و duplicate Termهای هر Attribute

Validation فعلی **package-level** است؛ اگر validation error وجود داشته باشد، mutation محصولات شروع نمی‌شود و خطاها در `validationErrors` گزارش می‌شوند. fileciteturn47file0L2-L6

## 4. Product matching

در Import، مقصد از Productهای موجود فروشگاه ساخته می‌شود و lookupهای مختلف برای match آماده می‌شوند. حالت `UPDATE_EXISTING` برای پیدا کردن Product موجود استفاده می‌شود و حالت `CREATE_NEW_DRAFT`/ایجاد جدید اجازه Create را می‌دهد.

Source Product ID یک شناسه مبدأ است و نباید به‌عنوان Destination ID نوشته شود مگر اینکه mapping همان فروشگاه آن را به یک Product موجود مقصد resolve کرده باشد.

> جزئیات دقیق اولویت match باید با implementation `findProductMatch` هم‌زمان نگه داشته شود؛ این سند عمداً الگوریتمی را که در کد مستقل و صریح به‌عنوان قرارداد عمومی ثبت نشده، به‌عنوان guarantee جدید معرفی نمی‌کند.

## 5. Variation mapping

Variation دارای Source ID، SKU، Attribute combination و Media reference است. Source Variation ID برای شناسایی/lookup مبدأ نگهداری می‌شود و ID مقصد هنگام mutation از Variation موجود یا `NEW_ID_PLACEHOLDER` گرفته می‌شود؛ بنابراین Source ID مستقیماً به‌عنوان Destination Variation ID نوشته نمی‌شود. مدل انتقال این جداسازی را حفظ می‌کند. fileciteturn38file0L2-L6

در حالت same-store، implementation می‌تواند Source Variation ID را برای پیدا کردن Variation موجود بررسی کند و در ادامه content-based matching نیز دارد. در cross-store، matching بر اساس محتوای Variation انجام می‌شود. این بخش باید با implementation فعلی هماهنگ بماند و نباید بدون تغییر کد، «mapping table پایدار» فرض شود.

Variation جدید به `savedProduct.id` مقصد متصل می‌شود؛ بنابراین parent Product مقصد مرجع mutation Variation است.

Variation اضافه مقصد حذف نمی‌شود؛ مسیر Import عملیات delete برای Variationهای unmatched ندارد.

## 6. SKU handling

SKUهای موجود مقصد در ابتدای Import جمع‌آوری می‌شوند و SKUهای Product و Variation برای جلوگیری از collision در مجموعه reservation استفاده می‌شوند.

Mutation برای entity جدید می‌تواند SKU را reserve کند. Reservation باید در failure آزاد شود و در success باقی بماند. هدف reservation جلوگیری از collision در همان Import است؛ این reservation یک database lock دائمی نیست.

SKU خالی یک identity مستقل و قابل‌اعتماد مبتنی بر SKU ایجاد نمی‌کند؛ بنابراین idempotency بدون SKU هنوز نباید به‌عنوان guarantee این سند تلقی شود.

## 7. Partial Failure و Result

پیاده‌سازی فعلی failure isolation را در چند سطح انجام می‌دهد:

- failure یک Product نباید loop سایر Productها را متوقف کند.
- failure یک Variation نباید Variationهای بعدی را متوقف کند.
- failure یک Media entry نباید Media entryهای دیگر یا loop Productها را متوقف کند.
- failure کلی در Media processing به `TransferMediaOutcome` تبدیل می‌شود تا Import Product ادامه پیدا کند.

Result شامل آمار جداگانه برای Product، Variation و Media است، از جمله:

- `created`
- `updated`
- `failed`
- `drafted`
- `variationsCreated`
- `variationsUpdated`
- `variationsFailed`
- `imagesUploaded`
- `imagesReused`
- `imagesFailed`
- `imagesUnused`
- `skuChanged`
- آمار Category / Attribute / Term
- `validationErrors`
- `importErrors`

مدل Result فعلی این فیلدها را به‌صورت مستقل نگه می‌دارد. fileciteturn40file0L2-L6

## 8. Media import و Dedup

Media Import قبل از Upload، Mediaهای مقصد را بررسی می‌کند. ترتیب فعلی تطبیق شامل:

1. تطبیق URL canonical شده همراه با بررسی hash محتوای مقصد.
2. Mediaای که در همین Import قبلاً با همان content hash resolve شده است.
3. تطبیق filename همراه با بررسی hash محتوای مقصد.
4. در صورت عدم تطبیق، Upload به MediaRepository.

برای Media مقصد، hash با دریافت محتوای URL محاسبه می‌شود. بنابراین Dedup بر اساس content hash قوی‌تر از filename/URL است، ولی اگر Media مقصد قابل دریافت نباشد، guarantee مطلق Dedup وجود ندارد و ممکن است Upload انجام شود.

هر Media entry در `try/catch` مستقل پردازش می‌شود. خطای Upload یا پردازش فقط همان entry را failed می‌کند. `uploaded` و `reused` فقط برای outcome واقعی Media محاسبه می‌شوند. fileciteturn51file0L2-L6

## 9. Category / Global Attribute / Term

Import قبل از Product mutation، Category mapping و Global Attribute mapping را resolve می‌کند.

- Categoryهای مقصد از repository خوانده می‌شوند.
- Category chain در مقصد resolve می‌شود.
- Global Attribute در same-store می‌تواند با ID حفظ شود.
- در حالت دیگر matching با slug/name انجام می‌شود.
- Termهای Global Attribute با name/slug مقصد resolve یا create می‌شوند.

این resolutionها بخشی از pipeline Import هستند؛ اما هنوز نباید به‌عنوان یک mapping database دائمی بین دو فروشگاه تلقی شوند.

## 10. Security / resource limits

ZIP reader این موارد را enforce می‌کند:

- Path traversal protection
- Duplicate entry detection
- Package size limit: `1 GiB`
- General entry limit: `50 MiB`
- `products.json` limit: `100 MiB`
- Product limit: `10,000`

این محدودیت‌ها علاوه بر validation ساختاری هستند. fileciteturn47file0L2-L6

## 11. چیزی که فعلاً implemented نیست

موارد زیر عمداً در این سند به‌عنوان قابلیت موجود معرفی نمی‌شوند:

- Export Media Dedup واقعی
- Idempotency تضمینی برای Import محصول بدون SKU
- Mapping table پایدار بین Source ID و Destination ID برای Importهای آینده
- checksum مستقل برای تمام entryهای package
- migration واقعی بین نسخه‌های مختلف schema/layout
- حذف خودکار Variationهای اضافه مقصد
- Partial validation در سطح هر Product به‌جای رد کل package در صورت validation error

این موارد می‌توانند در آینده اضافه شوند، اما تا زمانی که کدشان وجود نداشته باشد، نباید در documentation به‌عنوان implemented نوشته شوند.

## 12. Source of truth فایل‌ها

هسته فعلی این قابلیت در این فایل‌هاست:

- `RobustProductTransferService.kt` — orchestration Export/Import و Result aggregation
- `ProductTransferFormat.kt` — Format/Layout registry و ZIP path contract
- `ProductTransferArchive.kt` — ZIP read/write و image download
- `ProductTransferValidation.kt` — validation package/product
- `ProductTransferMedia.kt` — Media resolution/upload/dedup
- `ProductTransferModels.kt` — package و Result models
- `ProductTransferRepositoryReader.kt` — خواندن Product/Variation/Media/Category/Attribute از repository
- `SettingsScreen.kt` — entry pointهای UI برای Import/Export

این فایل باید هنگام تغییر implementation به‌روزرسانی شود تا با کد واقعی همگام بماند.

## 13. Versioning

`version` و `layoutVersion` دو مفهوم مستقل هستند. در حال حاضر فقط v1 قابل خواندن است و v1 هنوز temporary است. وقتی v1 نهایی شد، تغییر فیزیکی ناسازگار باید layout version جدید داشته باشد و reader سازگار برای نسخه‌های قبلی حفظ شود. fileciteturn46file0L2-L6

## 14. عدم وجود Credential در Package

Package نباید API key، consumer secret، password یا credential فروشگاه را ذخیره کند. `manifest.source` صرفاً metadata مربوط به فروشگاه مبدأ است.