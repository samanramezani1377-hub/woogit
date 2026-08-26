# WooGit — نقشه ساخت V1

این سند مرجع اصلی ترتیب و روش اجرای ساخت WooGit V1 است.

> **هدف:** قبل از ورود به پیاده‌سازی، مسیر ساخت، وابستگی مراحل، خروجی هر مرحله و معیار پایان آن مشخص باشد.

## 1. اصول حاکم بر ساخت

1. V1 ابتدا از پایه ساخته می‌شود؛ قابلیت‌های آینده فقط در حد مرز معماری آماده می‌مانند.
2. هیچ قابلیت آینده‌ای نباید مسیر اصلی V1 را پیچیده یا Launch را عقب بیندازد.
3. Core مستقل از UI و Android-specific code ساخته می‌شود.
4. Local-first اصل رفتار داده است؛ UI ابتدا Local State را می‌بیند و تغییرات برای Push وارد Pending Queue می‌شوند.
5. WooCommerce Backend اصلی V1 است؛ Backend اختصاصی WooGit و Companion Plugin در V1 ساخته نمی‌شوند.
6. هر مرحله باید قبل از ورود مرحله بعد، معیار پایان خود را پاس کند.
7. تست هم‌زمان با ساخت انجام می‌شود، نه فقط در انتهای پروژه.
8. هیچ تصمیم جدید مهمی خارج از `DECISION_TRACKER.md` معتبر نیست.

## 2. ترتیب کلی

```text
P0 Foundation
  ↓
P1 Core Contracts & Domain
  ↓
P2 Local Data
  ↓
P3 WooCommerce Integration
  ↓
P4 Sync Engine
  ↓
P5 Background Order Detection & Notifications
  ↓
P6 Security Hardening
  ↓
P7 UI Design System & Screens
  ↓
P8 UI ↔ Core Integration
  ↓
P9 Full Test Matrix
  ↓
P10 Beta / Hardening
  ↓
P11 V1 Release
```

این ترتیب به معنی جدا بودن کامل همه فعالیت‌ها نیست؛ هر مرحله تست و آماده‌سازی مرحله بعد را نیز انجام می‌دهد. اما **وابستگی معماری** باید حفظ شود.

---

## P0 — Foundation

### هدف
ساخت اسکلت فنی پروژه بدون پیاده‌سازی قابلیت‌های کسب‌وکار.

### کارها
- تعیین ساختار Moduleها بر اساس Core-Out و KMP.
- تنظیم Gradle و dependency management.
- تعیین package boundaries.
- تنظیم build variants و محیط‌های توسعه/آزمایش در صورت نیاز.
- تعیین lint/format و قوانین کدنویسی.
- ساخت CI پایه برای build و test.
- ایجاد تست پایه برای اطمینان از سالم بودن pipeline.
- تعیین سیاست logging و ممنوعیت Log کردن Credential/Secret.

### نباید وارد شود
- UI نهایی
- WooCommerce API implementation
- Sync logic
- Notification implementation
- AI
- Multi-Store implementation

### خروجی
یک پروژه خالی ولی buildable، testable و دارای مرزهای Module مشخص.

### معیار پایان
- Build موفق.
- Test pipeline موفق.
- Module boundaries قابل تشخیص.
- هیچ وابستگی UI-specific وارد Domain/Core نشده باشد.

---

## P1 — Core Contracts & Domain

### هدف
ساخت قلب مستقل پروژه و قراردادهایی که Android، UI و WooCommerce به آن متصل می‌شوند.

### کارها
- Domain Entityها برای Order، Product، Variation، Attribute و Store Connection.
- Repository interfaces.
- Use Case boundaries.
- Result/Error model.
- Entity Versioning abstraction.
- Commit/Version Provider abstraction با پیاده‌سازی V1 مبتنی بر `date_modified_gmt`.
- Pending Operation model.
- Sync contracts.
- Conflict model و قرارداد Resolution.
- Event boundary.
- Notification provider boundary.
- Store isolation boundary برای Multi-Store آینده.
- AI/Assistant boundary بدون implementation.

### نباید وارد شود
- Android Context در Domain.
- REST client implementation در Domain.
- Compose UI.
- Firebase/Push implementation.
- Backend اختصاصی.

### خروجی
Core قابل تست که بدون Android و WooCommerce واقعی بتوان منطق آن را تست کرد.

### معیار پایان
- Contractها مشخص و پایدار.
- Domain testها سبز.
- وابستگی معکوس رعایت شده باشد.
- `date_modified_gmt` در Core به شکل قابل جایگزینی مدل شده باشد.

---

## P2 — Local Data

### هدف
ایجاد منبع Local برای داده‌های مفید و صف پایدار عملیات.

### کارها
- انتخاب/پیاده‌سازی Local Database متناسب با KMP/Android.
- جدول/Entityهای موردنیاز.
- DAO/Data source.
- Local repository implementation.
- Pending Queue پایدار.
- ذخیره version state هر entity.
- migration strategy.
- حفظ Database و Pending Queue هنگام App Update.
- پاک‌سازی امن داده در سناریوهای تعریف‌شده.

### رفتار الزامی
```text
User Change
   ↓
Local DB update
   ↓
UI immediately reflects change
   ↓
Pending Operation
```

### معیار پایان
- CRUD محلی کامل برای محدوده V1.
- Pending operation بعد از restart باقی بماند.
- Migration تست شده باشد.
- هیچ Credential حساسی در Local Database معمولی ذخیره نشود.

---

## P3 — WooCommerce Integration

### هدف
ساخت اتصال واقعی و قابل اتکا به WooCommerce REST API.

### کارها
- Store URL connection.
- Consumer Key / Secret authentication.
- Secure transport.
- API client.
- Error mapping.
- Orders endpoints.
- Products endpoints.
- Variations.
- Attributes (Custom/Global).
- Customers/order address data.
- Order items.
- Discounts.
- Shipping.
- Payment information.
- Notes.
- Tracking fields.
- Product images/gallery.
- Search.
- Delete/cancel operations.
- Batch endpoints در جاهایی که مفید و سازگار باشند.

### معیار پایان
هر قابلیت API که در V1 قرار دارد باید حداقل یک integration test و مسیر خطای مشخص داشته باشد.

---

## P4 — Sync Engine

### هدف
وصل کردن Local-first به WooCommerce بدون از دست رفتن تغییر و بدون تکرار ناخواسته.

### جریان اصلی
```text
Local Mutation
    ↓
Pending Queue
    ↓
Sync Worker
    ↓
WooCommerce
    ↓
Server Version Check
    ↓
Success / Retry / Conflict
```

### کارها
- Queue ordering.
- Retry policy.
- Exponential/backoff strategy متناسب با نیاز.
- Timeout.
- Idempotency.
- Background continuation.
- Version comparison با `date_modified_gmt`.
- Conflict detection.
- Field-level merge در موارد امن.
- User conflict resolution برای موارد غیرقابل ادغام.
- حذف موفق عملیات از Queue.
- حفظ عملیات شکست‌خورده.
- جلوگیری از duplicate push.

### معیار پایان
سناریوهای زیر باید تست شده باشند:
- Offline هنگام تغییر.
- قطع شبکه حین Push.
- Retry موفق.
- Retry تکراری بدون duplicate mutation.
- تغییر Server هم‌زمان با Local.
- Conflict قابل merge.
- Conflict غیرقابل merge.
- App restart در میانه Queue.

---

## P5 — Background Order Detection & Notifications

### هدف
تشخیص سفارش جدید در V1 بدون Backend اختصاصی و بدون اتصال دائمی.

### کارها
- WorkManager.
- Periodic sync/polling مطابق محدودیت Android.
- تشخیص سفارش‌های جدید.
- جلوگیری از notification duplicate.
- ساخت notification payload شامل شماره سفارش، مبلغ، تعداد کالا، نام کالاها، تاریخ و ساعت.
- Local notification.
- رفتار هنگام App بسته.
- هماهنگی با Sync Engine.
- Status state برای Sync/Connection.

### محدودیت
Push واقعی Provider و Backend/Plugin در V1 ساخته نمی‌شود.

### معیار پایان
- سفارش جدید با polling تشخیص داده شود.
- اعلان تکراری تولید نشود.
- برنامه بدون سرویس دائمی کار کند.
- رفتار در شرایط محدودیت/تأخیر سیستم‌عامل مستند و تست شود.

---

## P6 — Security Hardening

### هدف
قفل کردن امنیت پیش از اتصال کامل UI و انتشار Beta.

### کارها
- Android Keystore برای Credentialهای حساس.
- عدم ذخیره Secret در plain text.
- جلوگیری از backup شدن Credential.
- HTTPS/TLS.
- Redaction در logs.
- مدیریت lifecycle اتصال فروشگاه.
- پاک‌سازی امن credential در disconnect.
- کنترل دسترسی قابلیت‌ها طبق مدل V1.
- بررسی memory/log leakage در مسیرهای حساس.

### معیار پایان
Credentialها در هیچ log، database معمولی، backup یا crash payload ناخواسته قابل مشاهده نباشند.

---

## P7 — UI Design System & Screens

### پیش‌شرط
Core، Local Data و قراردادهای اصلی باید پایدار شده باشند و Design System Hi-Fi مطابق Decision Tracker آماده باشد.

### هدف
ساخت UI کامل V1 روی Core موجود، نه ساخت منطق کسب‌وکار داخل UI.

### Design System
- Liquid Glass.
- RTL-first.
- LTR-ready.
- Typography.
- Spacing.
- Colors.
- Buttons.
- Cards.
- Forms.
- Modals.
- Lists.
- Status Dot.
- Pending/Synced/Failed states.
- Validation state.
- Conflict modal.
- Loading/empty/error states.

### Screens
- Connection/Onboarding.
- Dashboard.
- New order notification entry.
- Orders list.
- Order detail.
- Quick order actions.
- Full order edit.
- Products list/search.
- Quick product add.
- Full product edit.
- Variable Product.
- Variations.
- Attributes.
- Images/gallery.
- Settings/connection.

### معیار پایان
تمام جریان‌های V1 در Hi-Fi طراحی شده و سپس به Compose implementation تبدیل شوند؛ هیچ screen حیاتی بدون loading/empty/error/offline state نباشد.

---

## P8 — UI ↔ Core Integration

### هدف
وصل کردن UI به Use Case/State واقعی بدون انتقال Business Logic به Compose.

### کارها
- ViewModel/Presentation state.
- One-way state flow.
- User actions → Use Cases.
- Local state rendering.
- Sync state rendering.
- Error state rendering.
- Conflict resolution UI.
- Background refresh.
- Navigation.
- Deep links برای notification به صفحه سفارش.

### معیار پایان
هر جریان اصلی از UI تا Local/API و برگشت State قابل ردیابی باشد.

---

## P9 — Full Test Matrix

### تست‌ها
- Unit.
- Integration.
- UI.
- Database.
- WooCommerce API.
- Sync.
- Offline.
- Slow network.
- Network drop.
- Retry.
- Idempotency.
- Conflict.
- Notification.
- WorkManager.
- App restart.
- App update.
- Permission/error states.
- Large product/order/image cases.

### معیار خروج
تمام تست‌های اجباری V1 سبز باشند و هیچ blocker شناخته‌شده‌ای در سه محور اصلی باقی نماند.

---

## P10 — Beta / Hardening

### هدف
پیدا کردن مشکلاتی که در تست کنترل‌شده دیده نمی‌شوند.

### بررسی
- Crash rate.
- Startup.
- Memory.
- Battery.
- Large catalog.
- Large orders.
- Large images.
- Slow WooCommerce server.
- Weak mobile network.
- Background restrictions.
- Migration.
- Recovery.
- Duplicate operations.
- UX friction.

### معیار پایان
V1 در سناریوهای واقعی قابل اتکا باشد و هیچ مشکل blocker/critical باز باقی نماند.

---

## P11 — V1 Release

### کارهای نهایی
- Version freeze.
- Final migration check.
- Release build.
- Signing/security verification.
- Final regression.
- Documentation update.
- Known limitations.
- Release notes.
- Tag/Release در GitHub.

### تعریف Done برای V1
WooGit باید بتواند بدون Backend اختصاصی:

1. به یک فروشگاه WooCommerce متصل شود.
2. سفارش‌ها را محلی و از سرور مدیریت کند.
3. سفارش جدید را در پس‌زمینه تشخیص دهد و اعلان کند.
4. سفارش را ویرایش کند.
5. محصول ساده و متغیر را مدیریت کند.
6. Variation و Attribute را مدیریت کند.
7. تصاویر محصول را مدیریت کند.
8. تغییرات را Local-first انجام دهد.
9. تغییرات را با Pending Queue همگام کند.
10. در شبکه ضعیف/قطع‌شده تغییرات را از دست ندهد.
11. Conflict را طبق تصمیم V1 مدیریت کند.
12. Credentialها را امن نگهداری کند.
13. تست‌های اجباری V1 را پاس کند.

---

## 3. چیزهایی که عمداً در نقشه ساخت V1 نیستند

این موارد در مرز معماری در نظر گرفته می‌شوند اما در V1 پیاده‌سازی نمی‌شوند:

- Backend اختصاصی WooGit.
- Companion WordPress Plugin.
- Commit واقعی و Change History کامل.
- Push Provider واقعی برای ارسال از سمت فروشگاه.
- Multi-Store فعال.
- Multi-User.
- Multi-Device synchronization.
- AI/Assistant implementation.
- Notification Center کامل.
- User/Event Tracker فعال.
- Billing/License system.
- iOS UI/Release.

این حذف از V1 به معنی حذف از Product Vision نیست؛ این‌ها طبق `ROADMAP.md` برای آینده باقی می‌مانند.

## 4. قانون تغییر نقشه ساخت

اگر در طول پیاده‌سازی نیاز جدیدی پیدا شد:

1. ابتدا `DECISION_TRACKER.md` بررسی شود.
2. اگر تصمیم جدید است، همان‌جا ثبت شود.
3. سند تخصصی مرتبط به‌روزرسانی شود.
4. در صورت تغییر ترتیب مراحل، این فایل به‌روزرسانی شود.
5. قابلیت جدید بدون تعیین جایگاه در Build Plan وارد کد نشود.
6. تغییرات مستندات با Commit مستقل و پیام واضح ثبت شوند.

## 5. معیار کلی پایان پروژه

نقشه ساخت زمانی کامل اجرا شده است که مسیر زیر بدون شکستن قراردادهای Core برقرار باشد:

```text
User
 ↓
Presentation
 ↓
Use Case
 ↓
Core Domain
 ↓
Local Repository / Pending Queue
 ↓
Sync Engine
 ↓
WooCommerce API
 ↓
Version / Conflict Resolution
 ↓
Local State
 ↓
UI
```

و مسیر اعلان:

```text
WooCommerce
 ↓
Background Detection
 ↓
New Order Event
 ↓
Notification Boundary
 ↓
Android Notification
 ↓
Order Deep Link
```

**V1 زمانی آماده انتشار است که این دو مسیر پایدار، تست‌شده، امن و قابل بازیابی در برابر خطاهای شبکه باشند.**
