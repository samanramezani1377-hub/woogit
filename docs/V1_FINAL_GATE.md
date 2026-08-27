# WooGit V1 — Final Gate Audit

تاریخ: 2026-08-27

این سند وضعیت واقعی Repository را از روی کد موجود و آخرین GitHub Actions ثبت می‌کند. صرف وجود کد یا مستندات به‌تنهایی به‌عنوان تست دستگاه یا Release نهایی محسوب نمی‌شود.

## وضعیت فعلی

- [x] شاخه اصلی `main` دارای Android/KMP implementation واقعی است.
- [x] Core / Domain / Data / Presentation / Android composition root وجود دارند.
- [x] SQLDelight persistence و pending queue وجود دارند.
- [x] Local-first mutation برای Orders و Products به coordinator و queue متصل است.
- [x] WooCommerce connection، Orders، Products، Categories، Variations، Attributes، Terms و Media در مسیر V1 پیاده‌سازی شده‌اند.
- [x] WorkManager polling و new-order notification وجود دارند.
- [x] Android 13+ notification permission درخواست می‌شود.
- [x] Liquid Glass design primitives و shared glass surfaces در UI استفاده می‌شوند.
- [x] آخرین CI واقعی روی commit `4f8a271babebda881313b4b19e28e194cf61827f` سبز شده است: debug build، release build، unit tests، lint و APK verification همگی موفق بوده‌اند.
- [x] APK artifact توسط CI تولید و upload شده است.
- [x] Android edge-to-edge برای APIهای جدید فعال شده و `adjustResize` برای IME ثبت شده است.
- [x] مسیر order mutation دیگر خطاهای دائمی را به‌عنوان موفقیت کاذب گزارش نمی‌کند؛ فقط خطاهای قابل retry می‌توانند optimistic success باقی بمانند.

## V1 Functional Gate

### Connection
- [x] HTTPS-only connection
- [x] Credential storage boundary
- [x] Store persistence
- [x] Secure disconnect

### Orders
- [x] List
- [x] Search
- [x] Pagination
- [x] Detail
- [x] Customer / billing / shipping
- [x] Payment / transaction information
- [x] Shipping / discount information
- [x] Status update
- [x] Order note enqueue / remote push

### Products
- [x] List / search / pagination
- [x] Create
- [x] Edit
- [x] Delete
- [x] Category selection
- [x] Pricing / sale pricing
- [x] Stock state / stock management
- [x] Product images and gallery ordering
- [x] HTML-safe description preview
- [x] Variable products and variation routes

### Sync / Resilience
- [x] SQLDelight queue persistence
- [x] Operation claiming
- [x] Restart recovery of running operations
- [x] Retry with backoff
- [x] Conflict state boundary
- [x] Background sync worker
- [x] Startup/reconnect one-shot sync
- [x] Local cache fallback for primary list/detail reads

### Notifications
- [x] WorkManager polling
- [x] Deduplication store
- [x] Notification channel
- [x] Runtime permission request on Android 13+
- [x] Notification deep-link order ID

### UI
- [x] RTL Persian UI foundation
- [x] Loading / empty / error / offline / pending states
- [x] Shared Liquid Glass surfaces
- [x] Scrollable product editor
- [x] Category controls
- [x] Product HTML preview
- [x] Edge-to-edge and resize handling

## Remaining release verification

این موارد را نمی‌توان فقط با خواندن Repository «تأییدشده» اعلام کرد:

- [ ] تست روی دستگاه/Emulator واقعی برای اتصال WooCommerce
- [ ] تست واقعی create/edit/delete محصول با یک فروشگاه WooCommerce
- [ ] تست واقعی تغییر وضعیت سفارش و افزودن note
- [ ] تست واقعی Offline → Online و drain شدن queue
- [ ] تست واقعی notification و tap-to-order روی Android 13+
- [ ] تست process death / restart روی دستگاه
- [ ] بررسی UX نهایی در اندازه‌های مختلف صفحه
- [ ] تصمیم نهایی برای signing و Release APK/AAB production

## Gate decision

**Implementation Gate: GREEN**

**CI Gate: GREEN**

**Release Gate: PENDING REAL DEVICE / STORE VALIDATION**

هیچ تست یا نتیجه جعلی برای عبور از Gate ایجاد نشده است. موارد بخش آخر باید با اجرای واقعی تأیید شوند.
