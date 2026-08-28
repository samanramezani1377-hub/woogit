# WooGit — مستندات فنی

## ثبت نقاط مهم پروژه

### 2026-08-29 — افزودن عکس از گوشی

**وضعیت:** رفع شد و در CI تأیید شد.

**مشکل:** هنگام انتخاب تصویر از گالری گوشی، فایل با موفقیت روی WordPress/WooCommerce آپلود می‌شد، اما coroutine وابسته به Composition می‌توانست با خروج از Composition لغو شود و نتیجه‌ی Upload (`imageId` و `imageUrl`) به فرم محصول نرسد؛ در نتیجه کادر «آدرس تصویر» خالی می‌ماند.

**راه‌حل:** عملیات Upload تصویر از lifecycle مربوط به Composition خارج و به `ViewModel` و `viewModelScope` منتقل شد تا عملیات Upload مستقل از Composition ادامه پیدا کند و نتیجه‌ی واقعی Media (`id` و `source_url`) در state فرم قرار بگیرد.

**مسیر مورد انتظار:**

`Gallery گوشی → Upload → WordPress Media API → دریافت Media ID + URL → ViewModel state → imageUrl/imageId فرم → نمایش URL در کادر آدرس تصویر → Save محصول`

**Commit مرجع:**

`34d7578b2f9a3538fb04232a1fe9e2b3ac6176b2`

**Commit message:**

`fix(products): keep image upload alive outside composition`

**CI:** موفق (Run #694)

**نکته:** این commit نقطه مرجع برای رفع مشکل «افزودن عکس از گوشی» است و نباید بدون بررسی مسیر Upload و lifecycle دوباره تغییر کند.
