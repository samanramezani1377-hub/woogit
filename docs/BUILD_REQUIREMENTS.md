# WooGit V1 — نیازمندی‌های ساخت

این سند مرجع «چه چیزهایی باید برای ساخت و تکمیل V1 وجود داشته باشد؟» است. ترتیب اجرا در `BUILD_PLAN.md` و `EXECUTION_PLAN.md` و روش کنترل در `BUILD_CHECKLIST.md` است.

## قوانین کلی
- هر Requirement باید قابل تست و قابل پذیرش باشد.
- قابلیت آینده فقط در صورتی در V1 می‌آید که Decision Tracker صراحتاً آن را فعال کرده باشد.
- هیچ Requirement نباید فقط در UI پیاده شود؛ منطق باید در Core/Use Case قرار گیرد.
- همه مسیرهای داده باید Local-first باشند.
- Credential و Secret فقط در storage امن نگهداری شوند.
- **V1 بدون سرور اختصاصی WooGit است.** تنها سیستم remote ووکامرس فروشگاه است.
- قراردادهای دقیق V1 در `docs/V1_*` اسناد مرجع تکمیلی این فایل هستند.

## P0 — Foundation
### نیازهای معماری
- پروژه باید Core-Out باشد.
- Core نباید به Android/Compose وابسته باشد.
- KMP boundary باید از ابتدا مشخص باشد.
- Moduleها و dependency direction باید مشخص باشند.
- Build reproducible باشد.
- CI حداقل build و test را اجرا کند.
- lint/format و naming convention مشخص باشد.
- logging policy و redaction مشخص باشد.

### نیازهای تحویلی
- Gradle settings
- version/dependency strategy
- module skeleton
- CI workflow
- test skeleton
- README توسعه‌دهنده/راهنمای build در صورت نیاز

## P1 — Core & Domain
### مدل‌ها
- StoreConnection
- Order
- OrderItem
- Customer/Address
- Product
- ProductImage
- Variation
- Attribute
- SyncState
- PendingOperation
- EntityVersion
- Conflict
- DomainError
- Event
- NotificationIntent

### قراردادها
- Repository interfaces
- Use Cases
- SyncEngine interface
- VersionProvider
- ConflictResolver
- EventPublisher/Subscriber boundary
- NotificationProvider
- Store scope
- AI/Assistant boundary بدون implementation

### نیازمندی‌های رفتاری
- Domain مستقل از API و UI باشد.
- VersionProvider در V1 از `date_modified_gmt` استفاده کند.
- جایگزینی آن در آینده بدون تغییر Domain ممکن باشد.
- Errorها typed و قابل نمایش در Presentation باشند.

## P2 — Local Data
- **V1 local database: SQLDelight.**
- Database برای داده‌های لازم V1.
- CRUD برای Orders/Products/Variations/Attributes و Store state.
- Pending Queue پایدار.
- Queue state: pending/running/succeeded/failed/conflict.
- Version state.
- Migration.
- Restart recovery.
- Transactionهای لازم.
- Cache/freshness policy.
- عدم ذخیره Secret در DB عادی.
- امکان پاک‌سازی Store data هنگام disconnect طبق سیاست امنیتی.

## P3 — WooCommerce Integration
### Connection
- Store URL validation.
- Consumer Key/Secret.
- Connection test.
- Authentication failure mapping.
- TLS/HTTPS enforcement.

### Orders
- List.
- Search/filter/sort موردنیاز V1.
- Detail.
- Items.
- Customer/address.
- Payment.
- Shipping.
- Discounts.
- Notes.
- Status.
- Edit.
- Cancel/delete طبق تصمیم V1.

### Products
- List/search.
- Create.
- Edit.
- Delete.
- Simple product.
- Variable product.
- Variations CRUD.
- Global/custom attributes.
- Images.
- Gallery.
- Pricing/stock/basic product fields طبق نیازمندی محصول.

### API requirements
- Timeout.
- Pagination.
- Rate/HTTP error mapping.
- Serialization validation.
- Server-side validation mapping.
- Retry-safe request classification.
- Batch operations where useful and supported, with per-entity failure tracking.

## P4 — Sync Engine
- Local mutation first.
- Operation enqueue.
- Deterministic queue ordering.
- Retry policy.
- Backoff.
- Timeout.
- Stable operation identity.
- Idempotency/reconciliation strategy for ambiguous mutation outcomes.
- Version comparison.
- Conflict detection.
- Safe merge where explicitly proven.
- Explicit user conflict for unsafe merge.
- Recovery after process death.
- No silent data loss.
- No silent overwrite of newer server data.
- Sync status observable by UI.

## P5 — Notifications
- Background worker based on Android-supported scheduling.
- Periodic order detection.
- New-order identification.
- Deduplication.
- Local notification.
- Notification payload.
- Tap → order deep link.
- App closed behavior where Android permits background execution.
- Explicit handling of force-stop/platform limitations.
- No permanent foreground connection.
- **No custom backend/provider in V1.**

## P6 — Security
- Android Keystore or equivalent secure credential storage.
- Secrets excluded from normal backups where technically possible.
- No secrets in logs.
- HTTPS/TLS.
- Secure disconnect.
- Secure store switching.
- Error messages must not expose credentials or sensitive headers.
- Crash/log diagnostics must redact sensitive fields.

## P7 — UI/Design System
### Global
- RTL-first.
- LTR-ready.
- Liquid Glass visual language طبق تصمیم طراحی.
- Consistent typography/spacing/components.
- Accessibility basics.
- Loading/empty/error/offline states.
- Pending/Synced/Failed indicators.
- Hi-Fi specification before feature implementation.

### Screens
1. Onboarding/Connection
2. Dashboard
3. Orders list
4. Order detail
5. Quick order actions
6. Full order edit
7. Products list/search
8. Quick product add
9. Full product edit
10. Variable product
11. Variations
12. Attributes
13. Images/gallery
14. Settings/connection

## P8 — Presentation Integration
- ViewModel/state holder per feature.
- One-way data flow.
- Use Case invocation.
- Local-first rendering.
- Sync state rendering.
- Error rendering.
- Conflict resolution flow.
- Notification deep links.
- State restoration where required.
- No business rules duplicated in Compose.

## P9 — Test Requirements
### Unit
- Domain rules.
- Error mapping.
- Versioning.
- Queue.
- Retry.
- Conflict.
- Idempotency/reconciliation.

### Integration
- Database.
- API.
- Repository.
- Sync.
- Worker.
- Notification deduplication.

### UI
- Critical navigation.
- Orders.
- Order edit.
- Products.
- Product edit.
- Variation.
- Connection.
- Error/offline/conflict states.

### Resilience
- Offline.
- Slow network.
- Network drop.
- Server 4xx/5xx.
- Timeout.
- Process death.
- Restart.
- Update/migration.
- Duplicate request.
- Ambiguous mutation outcome.
- Concurrent server/local mutation.

## P10 — Beta/Hardening
- Crash-free critical flows.
- Startup acceptable.
- Memory stable.
- Battery impact acceptable.
- Large catalog behavior acceptable.
- Large order behavior acceptable.
- Large image handling acceptable.
- Weak server/network behavior acceptable.
- Recovery from failed sync verified.
- UX blockers removed.

## P11 — Release
- Release build reproducible.
- Signing configured securely.
- Versioning finalized.
- Migration verified.
- Regression suite green.
- Known limitations documented.
- V1 scope verified against Product Vision, Roadmap and Decision Tracker.
- Release notes prepared.
- Git tag/release created.

## V1 Non-Requirements
- Backend اختصاصی WooGit.
- Companion Plugin.
- Real Commit history.
- Real Push provider.
- Multi-Store فعال.
- Multi-User.
- Multi-Device sync.
- AI implementation.
- Billing/License.
- iOS release.
- Notification Center کامل.
