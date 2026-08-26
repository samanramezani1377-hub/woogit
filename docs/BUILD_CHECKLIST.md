# WooGit V1 — چک‌لیست بررسی و کنترل ساخت

این سند مرجع بررسی هر مرحله است و اکنون نتیجه آخرین بررسی واقعی Repository را نیز ثبت می‌کند.

## وضعیت‌ها
- `[ ]` بررسی نشده
- `[~]` در حال بررسی
- `[x]` تأیید شده
- `[!]` مشکل/Blocker
- `[-]` خارج از محدوده V1

> مواردی که نیازمند اجرای کد، Build، API، دستگاه یا Test Suite هستند تا زمان وجود implementation هرگز صرفاً بر اساس مستندات تأیید نمی‌شوند.

## وضعیت کلی — 2026-08-26

Repository فعلی در مرحله مستندسازی و تثبیت پیش از کدنویسی است. Tree فعلی شامل `README.md` و مستندات پروژه است و Android/KMP source، Gradle build، test suite و CI implementation هنوز وجود ندارند. بنابراین بررسی معماری و Scope از روی اسناد ممکن است، اما بررسی اجرایی هنوز قابل تأیید نیست.

**نتیجه کلی: P0 هنوز Gate نشده و مراحل اجرایی بعد از آن Not Started/Blocked هستند.**

---

# P0 — Foundation

## بررسی معماری
- [x] Core-Out به‌عنوان معماری اصلی تعریف شده است؛ implementation هنوز وجود ندارد.
- [x] جهت وابستگی Moduleها در Architecture/Build Plan تعریف شده است؛ Module واقعی هنوز وجود ندارد.
- [x] Core مستقل از Android/Compose تعریف شده است.
- [x] KMP boundary تعریف شده است.
- [x] قابلیت‌های آینده از V1 جدا شده‌اند.

## بررسی Build
- [!] Clean build — قابل اجرا نیست؛ Gradle project وجود ندارد.
- [!] Debug build — قابل اجرا نیست.
- [!] Test task — test infrastructure وجود ندارد.
- [!] CI build/test — workflow اجرایی وجود ندارد.
- [x] Dependencyهای runtime هنوز وجود ندارند؛ بنابراین dependency اضافی برای حذف مشاهده نشد.

## بررسی کیفیت
- [!] Lint/format — قابل اجرا نیست؛ source/build configuration وجود ندارد.
- [x] Logging policy در اسناد تعریف شده است.
- [x] Secret hardcoded در فایل‌های مستنداتی بررسی‌شده مشاهده نشد.

### Gate P0
- [!] **BLOCKED** — Foundation کدی، Build، Test و CI هنوز ایجاد نشده‌اند.

---

# P1 — Core & Domain

## بررسی مدل‌ها
- [x] Order/Product/Variation/Attribute/Store/Sync/PendingOperation در نیازمندی‌ها تعریف شده‌اند.
- [!] سازگاری مدل Order با API واقعی — نیازمند implementation و API test.
- [!] Product/Variation/Attribute implementation — وجود ندارد.
- [x] Store scope و Sync/Pending boundaries در معماری تعریف شده‌اند.

## بررسی معماری
- [x] Repository Contract، Use Case، جداسازی API/Domain و VersionProvider در اسناد تعریف شده‌اند.
- [x] V1 از `date_modified_gmt` استفاده می‌کند.
- [x] Commit واقعی در V1 نیست.
- [x] Conflict و Event/Notification boundaries تعریف شده‌اند.

## بررسی تست
- [!] Domain rules — implementation/test وجود ندارد.
- [!] Error model — implementation/test وجود ندارد.
- [!] Version comparison — implementation/test وجود ندارد.
- [!] Queue state transitions — implementation/test وجود ندارد.

### Gate P1
- [!] **BLOCKED تا P0 و ایجاد Core implementation.**

---

# P2 — Local Data

## Database
- [x] Schema/Keys/Index/Transaction/Migration به‌عنوان نیاز مشخص شده‌اند.
- [!] Schema واقعی — Database implementation وجود ندارد.
- [!] Migration — وجود ندارد.

## Queue
- [x] Pending Queue و stateهای آن در Requirements تعریف شده‌اند.
- [!] Restart persistence — implementation ندارد.
- [!] Deterministic transitions — قابل تست نیست.
- [!] Failed operation retention — قابل تست نیست.
- [!] Conflict state persistence — قابل تست نیست.

## Security
- [x] عدم ذخیره Credential در DB عادی الزام شده است.
- [x] پاک‌سازی Store data الزام شده است.
- [!] Backup behavior واقعی — implementation وجود ندارد.

## Tests
- [!] CRUD / Restart / Migration / Invalid-state tests — وجود ندارند.

### Gate P2
- [!] **NOT STARTED / BLOCKED BY P0-P1.**

---

# P3 — WooCommerce Integration

## Connection
- [x] URL، Credential، HTTPS، Timeout و Server failure در Requirements تعریف شده‌اند.
- [!] تست واقعی Connection — API client وجود ندارد.

## Orders
- [x] List، Search/Filter، Detail، Items، Customer/Address، Payment، Shipping، Discount، Notes، Status و Edit در Scope تعریف شده‌اند.
- [!] API implementation و integration test — وجود ندارند.

## Products
- [x] List/Search/Create/Edit/Delete، Simple/Variable، Variations، Attributes، Images/Gallery در Scope تعریف شده‌اند.
- [!] API implementation و integration test — وجود ندارند.

## API Error Handling
- [x] Pagination، Serialization، 4xx، 5xx، Timeout، Rate Limit و Malformed Response به‌عنوان نیاز ثبت شده‌اند.
- [!] Error mapping واقعی — implementation وجود ندارد.

### Gate P3
- [!] **NOT STARTED / BLOCKED BY P0-P2.**

---

# P4 — Sync Engine

## جریان اصلی
- [x] Local → Queue → Worker → WooCommerce → Version/Conflict در Build Plan تعریف شده است.
- [!] Local mutation واقعی و Queue/Worker — implementation ندارد.
- [!] Server state update — تست نشده و implementation ندارد.

## Retry
- [x] Retry، Backoff، Timeout و Idempotency نیاز قطعی هستند.
- [!] اجرای واقعی و جلوگیری از duplicate mutation — تست نشده.

## Conflict
- [x] Version check، merge امن و conflict غیرقابل merge تعریف شده‌اند.
- [!] Conflict واقعی و جلوگیری از silent overwrite — تست نشده.

## Recovery
- [x] Process death، Restart، Network drop، Server down، Offline طولانی و Queue بزرگ در Matrix تعریف شده‌اند.
- [!] اجرای واقعی همه سناریوها — implementation وجود ندارد.

### Gate P4
- [!] **NOT STARTED / BLOCKED BY P0-P3.**

---

# P5 — Background Detection & Notifications

## Background
- [x] WorkManager و عدم استفاده از اتصال دائمی در Scope مشخص شده‌اند.
- [!] WorkManager implementation/configuration وجود ندارد.
- [!] Battery/network behavior تست نشده است.

## Order Detection
- [x] New Order Detection و deduplication تعریف شده‌اند.
- [!] تشخیص واقعی سفارش جدید وجود ندارد.
- [!] duplicate notification تست نشده است.

## Notification
- [x] Payload، tap action و deep link تعریف شده‌اند.
- [!] Notification واقعی روی دستگاه تست نشده است.

### Gate P5
- [!] **NOT STARTED / BLOCKED BY P0-P4.**

---

# P6 — Security

## Credential
- [x] Secure Storage/Keystore، عدم Log/Crash/Backup ناخواسته Secret و Secure Disconnect الزام شده‌اند.
- [!] implementation امنیتی وجود ندارد؛ بنابراین هیچ مورد اجرایی تأیید نشده است.

## Network
- [x] HTTPS/TLS و عدم چاپ Header حساس الزام شده است.
- [!] Network Security implementation/test وجود ندارد.

### Gate P6
- [!] **NOT STARTED / BLOCKED BY implementation.**

---

# P7 — UI / Design System

## Design System
- [x] Liquid Glass، RTL-first، LTR-ready، Typography، Spacing، Components و Accessibility در نقشه ساخت تعریف شده‌اند.
- [!] Hi-Fi Design واقعی در Repository وجود ندارد.
- [!] Compose implementation وجود ندارد.

## States
- [x] Loading/Empty/Error/Offline/Pending/Synced/Failed/Conflict تعریف شده‌اند.
- [!] طراحی و implementation واقعی stateها تأیید نشده است.

## Screens
- [x] Screens اصلی V1 در Build Plan/Requirements مشخص شده‌اند.
- [!] Screen واقعی وجود ندارد.

### Gate P7
- [!] **NOT STARTED** — ابتدا Hi-Fi Design و سپس implementation.

---

# P8 — UI ↔ Core

## State
- [x] Local-first rendering، Use Case mutation، Sync status، Error و Conflict flow تعریف شده‌اند.
- [!] implementation واقعی وجود ندارد.

## Architecture
- [x] Business Logic در Compose و API call مستقیم از UI ممنوع شده‌اند.
- [!] ViewModel/Presentation implementation وجود ندارد.

### Gate P8
- [!] **NOT STARTED / BLOCKED BY P1-P7.**

---

# P9 — Full Test Matrix

## Functional
- [x] Connection، Orders، Order Editing، Products، Variations، Attributes، Images و Notifications در Matrix تعریف شده‌اند.
- [!] اجرای تست‌ها ممکن نیست؛ implementation وجود ندارد.

## Resilience
- [x] Offline، Slow Network، Drop، Timeout، 4xx، 5xx، Restart، Update، Migration، Duplicate و Conflict تعریف شده‌اند.
- [!] اجرای واقعی وجود ندارد.

## Performance
- [x] Startup، Memory، Large Catalog، Large Order، Large Image و Queue Performance تعریف شده‌اند.
- [!] Benchmark/Measurement وجود ندارد.

### Gate P9
- [!] **NOT STARTED / BLOCKED BY implementation.**

---

# P10 — Beta / Hardening

- [!] Real WooCommerce stores — Not Started.
- [!] Multiple hosting/server conditions — Not Started.
- [!] Weak mobile network — Not Started.
- [!] Background restrictions — Not Started.
- [!] Large data — Not Started.
- [!] Crash monitoring — Not Started.
- [!] UX triage — Not Started.
- [!] Critical/High issue resolution — Not Started.

### Gate P10
- [!] **NOT STARTED.**

---

# P11 — Release

- [x] تطبیق با Product Vision، Roadmap و Decision Tracker در فرآیند Release تعریف شده است.
- [!] Scope اجرایی نهایی — کد وجود ندارد.
- [!] Release build — وجود ندارد.
- [!] Signing verification — انجام نشده.
- [!] Migration verification — انجام نشده.
- [!] Final regression — انجام نشده.
- [!] Known limitations — نهایی نشده.
- [!] Release notes — آماده نیست.
- [!] GitHub release/tag V1 — وجود ندارد.

## V1 Final Gate
- [!] New Order Notification — Not Implemented.
- [!] Orders Management — Not Implemented.
- [!] Products Management — Not Implemented.
- [!] Local-first — Not Implemented.
- [!] Sync/Retry/Conflict — Not Implemented.
- [!] Security implementation — Not Implemented.
- [!] No Silent Data Loss — تا تست Sync Engine قابل اثبات نیست.
- [!] No Critical/High blocker — تا Beta قابل اثبات نیست.

---

# بررسی‌های اجباری مشترک

- [x] Product Vision با Build Plan/Requirements هم‌راستا است.
- [x] Roadmap مرز V1/Future را مشخص کرده است.
- [x] Decision Tracker Source of Truth تصمیم‌های V1 است.
- [x] Architecture boundary در اسناد تعریف شده است.
- [x] قابلیت‌های آینده از implementation V1 جدا شده‌اند.
- [!] قابلیت‌های اجرایی هنوز قابل تست نیستند.
- [x] Offline/Network failure در Requirements و Checklist پوشش داده شده است.
- [x] Credential security به‌عنوان Requirement ثبت شده است.
- [x] Project Map محل نگهداری مستندات را مشخص کرده است.

# نتیجه نهایی بررسی فعلی

## P0 = BLOCKED

دلیل Blocker کمبود تصمیم یا مستندات نیست؛ Repository هنوز وارد مرحله کدنویسی نشده و Foundation اجرایی ندارد.

**اقدام بعدی فقط P0 — Foundation است.** تا Gate واقعی P0 پاس نشده، نباید P1/P2/P3 یا UI را به‌صورت اجرایی شروع کنیم.

### اقدامات لازم برای باز شدن P0
1. ایجاد Android/KMP project skeleton.
2. ایجاد Gradle/module structure.
3. ایجاد package boundaries طبق Architecture.
4. ایجاد test infrastructure.
5. ایجاد CI build/test.
6. اجرای Clean/Debug/Test build.
7. Architecture Review روی ساختار واقعی.
8. فقط پس از عبور واقعی همه موارد، Gate P0 به `[x]` تغییر کند.
