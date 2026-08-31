# WooGit — Implementation Status Snapshot

> این سند یک snapshot از رفتارهای پیاده‌سازی‌شده در کد فعلی `main` است. Roadmap یا قابلیت آینده را implemented فرض نمی‌کند.

## Product Import / Export

پیاده‌سازی فعلی انتقال محصول از طریق `RobustProductTransferService` انجام می‌شود و از repositoryهای Product، Variation، Media، Category و Attribute استفاده می‌کند.

### Export

- خروجی با فرمت `.woogit` و container از نوع ZIP ساخته می‌شود.
- سقف Export برابر `10,000` Product است.
- Productهای Variable همراه Variationهایشان export می‌شوند.
- Categoryها و Global Attribute/Termهای موردنیاز داخل package قرار می‌گیرند.
- تصاویر Product و Variation داخل `media/` قرار می‌گیرند.
- سقف هر Media entry برابر `50 MiB` و سقف کل package برابر `1 GiB` است.
- Export در `Dispatchers.IO` اجرا می‌شود و progress callback دارد.
- اگر یک Media ضروری قابل دریافت نباشد، Export موفق گزارش نمی‌شود و package ناقص نباید به‌عنوان خروجی موفق تحویل شود.

### Import

ترتیب کلی implementation فعلی:

```text
Read ZIP
  ↓
Package validation
  ↓
Media resolve/upload
  ↓
Read destination products
  ↓
Resolve categories / global attributes / terms
  ↓
Match product
  ↓
Create / Update product
  ↓
Draft/Pending safety (در حالت Draft)
  ↓
Match + Create / Update variations
  ↓
Aggregate result
```

- Import package قبل از mutation اعتبارسنجی می‌شود.
- Validation failure باعث شروع mutation نمی‌شود.
- Import با `5` عملیات همزمان برای Productها (`IMPORT_CONCURRENCY = 5`) اجرا می‌شود.
- سقف Variation برای هر Product برابر `10,000` است.
- failure یک Product نباید loop سایر Productها را متوقف کند.
- failure یک Variation نباید Variationهای دیگر را متوقف کند.
- failure Media به outcome خطا تبدیل می‌شود و Import تا حد امکان ادامه پیدا می‌کند.

### Import Modes

UI فعلی سه حالت دارد:

1. `UPDATE_EXISTING` — تطبیق Product موجود و Update؛ Product جدید نیز طبق منطق انتقال ایجاد می‌شود.
2. `CREATE_NEW` — Productها بدون تطبیق با Productهای قبلی به‌عنوان جدید ایجاد می‌شوند.
3. `CREATE_NEW_DRAFT` — Productهای جدید با وضعیت Draft ایجاد می‌شوند.

در حالت `CREATE_NEW_DRAFT`:

- ابتدا Product با status `DRAFT` درخواست می‌شود.
- اگر نتیجه Draft نباشد، یک تلاش برای `PENDING` انجام می‌شود.
- وضعیت واقعی Product بعد از mutation بررسی می‌شود.
- اگر Draft و Pending اعمال نشوند و Product منتشر شود، این وضعیت در نتیجه ثبت می‌شود.
- UI یک گزینه صریح برای اجازه باقی‌ماندن Product منتشرشده دارد.
- در حالت پیش‌فرض، برای جلوگیری از انتشار ناخواسته، Productهایی که به‌اشتباه منتشر شده‌اند از طریق cleanup بررسی می‌شوند.

### Import Options

UI فعلی این گزینه‌های اختیاری را ارائه می‌کند و به‌صورت پیش‌فرض خاموش هستند:

- ایجاد Category در صورت نبودن در مقصد.
- ایجاد Global Attribute/Term در صورت نبودن در مقصد.
- Upload همه تصاویر بدون بررسی قبلی Media Library.

### Matching و Identity

- Source Product ID و Destination Product ID مستقل هستند.
- Source Variation ID مستقیماً به Destination Variation ID تبدیل نمی‌شود.
- برای Product موجود، lookupهای ID/SKU/fingerprint در Service استفاده می‌شوند.
- برای Variation، same-store می‌تواند Source ID را بررسی کند و در ادامه content-based matching انجام می‌شود.
- Variation جدید با Product مقصد (`savedProduct.id`) ساخته می‌شود.
- Variationهای اضافه مقصد حذف نمی‌شوند.

### SKU Safety

- SKUهای مقصد قبل از mutation در مجموعه `usedSku` جمع می‌شوند.
- SKU جدید قبل از ایجاد entity رزرو می‌شود.
- Reservation با `Mutex` در عملیات همزمان محافظت می‌شود.
- در failure/exception رزرو آزاد می‌شود.
- reservation یک database lock دائمی نیست.

### Result و Progress

Import/Export progress از `ProductTransferProgress` به UI منتقل می‌شود.

UI فعلی هنگام انتقال تعداد فعلی و کل را نمایش می‌دهد و برای Import/Export وضعیت پردازش را نشان می‌دهد.

Result Import آمار مستقل برای Product، Variation، Media و taxonomy دارد و خطاهای validation را از import/mutation errors جدا نگه می‌دارد.

## نکات صریحاً خارج از Guarantee فعلی

این snapshot نباید این موارد را implemented/guaranteed معرفی کند:

- Idempotency کامل Product بدون SKU.
- Persistent Source → Destination mapping database.
- Export Media Dedup قطعی.
- Rollback تراکنشی کل package.
- حذف خودکار Variationهای اضافه مقصد.
- Migration خودکار نسخه‌های آینده `.woogit`.
- انتقال تضمینی تمام metadataهای اختصاصی WooCommerce/pluginها.

## قواعد نگهداری

- این سند باید بعد از تغییر behaviorهای Import/Export با کد sync شود.
- تغییر مستندات نباید به‌جای رفع bug باعث حذف capability شود.
- برای Bug Fix، تغییر کد باید حداقلی باشد و diff قبل از commit بررسی شود.
- وضعیت سبز CI به‌تنهایی اثبات behavior کامل Import/Export نیست؛ تست واقعی و outcome mutation ملاک behavior است.
