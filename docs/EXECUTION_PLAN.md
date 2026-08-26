# WooGit V1 — برنامه اجرایی

این سند فهرست اجرایی ساخت V1 است؛ از Foundation تا Release. هر Task یک واحد قابل انجام، بررسی و پذیرش است.

## وضعیت Taskها
- `[ ]` انجام نشده
- `[~]` در حال انجام
- `[x]` انجام شده و بررسی شده
- `[!]` مشکل/Blocker
- `[-]` خارج از V1

## قوانین اجرا
1. ترتیب Taskها عمداً از پایین‌ترین لایه به بالاترین لایه است.
2. Task بعدی فقط وقتی شروع می‌شود که Dependencyهایش قابل استفاده باشند.
3. «کدنویسی شد» به معنی Done نیست؛ هر Task باید خروجی و Review داشته باشد.
4. تست همان مرحله نوشته می‌شود و به پایان پروژه موکول نمی‌شود.
5. قابلیت Future فقط در مرز معماری باقی می‌ماند مگر Decision Tracker آن را برای V1 فعال کند.
6. هر تغییر معماری مهم باید در `DECISION_TRACKER.md` ثبت شود.

---

# E0 — Foundation

## E0.01 — ایجاد Project Skeleton
**پیش‌نیاز:** هیچ

- [ ] ایجاد Android/KMP project پایه.
- [ ] تعیین package root.
- [ ] تعیین application/module identifiers.
- [ ] اجرای اولین Gradle sync.

**خروجی:** پروژه قابل باز شدن و Build اولیه.

**Done وقتی:** Gradle sync و skeleton build بدون خطا انجام شود.

## E0.02 — تعریف Module Boundaries
**پیش‌نیاز:** E0.01

- [ ] تعریف Core/Domain module.
- [ ] تعریف Data/Infrastructure boundary.
- [ ] تعریف Presentation/UI boundary.
- [ ] تعریف App/Android entry point.
- [ ] ثبت dependency direction.

**Done:** هیچ dependency از Core به UI/Android وجود نداشته باشد.

## E0.03 — Kotlin/KMP/Compose Configuration
**پیش‌نیاز:** E0.01

- [ ] تنظیم Kotlin.
- [ ] تنظیم KMP source sets.
- [ ] تنظیم Compose در محل مناسب.
- [ ] تعیین minimum/target SDK طبق نیاز V1.
- [ ] تنظیم Java/Kotlin toolchain.

**Done:** build همه targetهای V1 بدون warning/blocker مهم.

## E0.04 — Dependency Management
**پیش‌نیاز:** E0.03

- [ ] تعیین نسخه‌های کتابخانه‌ها.
- [ ] version catalog/dependency convention.
- [ ] جلوگیری از dependencyهای غیرضروری.
- [ ] ثبت دلیل dependencyهای معماری مهم.

**Done:** dependency graph قابل بررسی و تکرارپذیر باشد.

## E0.05 — Quality Tooling
**پیش‌نیاز:** E0.04

- [ ] Kotlin formatting.
- [ ] Lint/static analysis.
- [ ] naming/package rules.
- [ ] baseline در صورت نیاز.
- [ ] اجرای local quality check.

**Done:** quality taskها قابل اجرا باشند.

## E0.06 — Test Infrastructure
**پیش‌نیاز:** E0.03

- [ ] Unit test setup.
- [ ] Shared test utilities.
- [ ] Fake/test data strategy.
- [ ] Test naming convention.

**Done:** حداقل یک test واقعی سبز باشد.

## E0.07 — CI Foundation
**پیش‌نیاز:** E0.05,E0.06

- [ ] GitHub Actions workflow.
- [ ] Build.
- [ ] Unit tests.
- [ ] Quality checks.
- [ ] Failure visibility.

**Done:** یک اجرای CI کامل build/test/quality را پاس کند.

## E0.08 — Foundation Review Gate
**پیش‌نیاز:** E0.01–E0.07

- [ ] Architecture review.
- [ ] Dependency review.
- [ ] Build review.
- [ ] Test review.
- [ ] CI review.
- [ ] Documentation update.

**Gate:** فقط پس از تأیید همه موارد وارد E1 شویم.

---

# E1 — Core Foundation

## E1.01 — Core Package Structure
**پیش‌نیاز:** E0.08

- [ ] domain/entity
- [ ] domain/usecase
- [ ] domain/repository
- [ ] domain/sync
- [ ] domain/error
- [ ] domain/event boundaries

## E1.02 — Identity & Time Primitives
**پیش‌نیاز:** E1.01

- [ ] Entity ID strategy.
- [ ] Store ID strategy.
- [ ] Timestamp representation.
- [ ] UTC policy.

## E1.03 — Result & Domain Error
**پیش‌نیاز:** E1.01

- [ ] Typed domain errors.
- [ ] Recoverable vs non-recoverable classification.
- [ ] Mapping contract for Presentation.

## E1.04 — Version Provider
**پیش‌نیاز:** E1.02

- [ ] Version abstraction.
- [ ] V1 `date_modified_gmt` implementation contract.
- [ ] Future Commit provider boundary.

## E1.05 — Repository Contracts
**پیش‌نیاز:** E1.01

- [ ] OrderRepository.
- [ ] ProductRepository.
- [ ] StoreRepository.
- [ ] Sync/Pending repository contracts.

## E1.06 — Use Case Contracts
**پیش‌نیاز:** E1.05

- [ ] Order use cases.
- [ ] Product use cases.
- [ ] Connection use cases.
- [ ] Sync use cases.

## E1.07 — Event/Notification Boundaries
**پیش‌نیاز:** E1.01

- [ ] Event model.
- [ ] Event publisher/subscriber boundary.
- [ ] Notification intent/provider boundary.
- [ ] No provider implementation yet.

## E1.08 — Store/Future Boundaries
**پیش‌نیاز:** E1.01

- [ ] Store scope.
- [ ] Multi-Store-ready boundary without active Multi-Store.
- [ ] AI/Assistant boundary without implementation.

## E1.09 — Core Unit Tests
**پیش‌نیاز:** E1.02–E1.08

- [ ] IDs/time.
- [ ] Errors.
- [ ] Version rules.
- [ ] Contract behavior tests where applicable.

## E1.10 — Core Review Gate
**پیش‌نیاز:** E1.09

- [ ] Dependency direction verified.
- [ ] No Android/UI dependency.
- [ ] Version abstraction verified.
- [ ] Future boundaries verified.
- [ ] Tests green.

---

# E2 — Domain Models

## E2.01 — Store Connection Model
- [ ] Store identity.
- [ ] URL.
- [ ] Connection state.
- [ ] Credential reference, never raw secret in domain persistence.

## E2.02 — Order Models
- [ ] Order.
- [ ] OrderItem.
- [ ] Customer.
- [ ] Billing/Shipping Address.
- [ ] Payment.
- [ ] Shipping.
- [ ] Discount.
- [ ] Notes.
- [ ] Status.

## E2.03 — Product Models
- [ ] Product.
- [ ] ProductImage.
- [ ] ProductGallery.
- [ ] Pricing.
- [ ] Stock fields required by V1.

## E2.04 — Variable Product Models
- [ ] Variation.
- [ ] Variation attributes.
- [ ] Variation pricing/stock fields.

## E2.05 — Attribute Models
- [ ] Global Attribute.
- [ ] Custom Attribute.
- [ ] Attribute terms/value representation.

## E2.06 — Sync Models
- [ ] SyncState.
- [ ] PendingOperation.
- [ ] Operation type.
- [ ] Retry metadata.
- [ ] EntityVersion.
- [ ] Conflict.

## E2.07 — Mapping/Validation Tests
- [ ] Valid model tests.
- [ ] Invalid data tests.
- [ ] Version/timestamp tests.

## E2.08 — Domain Model Gate
- [ ] API needs cross-checked.
- [ ] Model ownership clear.
- [ ] No UI-specific model leakage.
- [ ] Tests green.

---

# E3 — Local Data

## E3.01 — Select Database Strategy
- [ ] Confirm KMP/Android-compatible database.
- [ ] Verify transaction support.
- [ ] Verify migration support.
- [ ] Verify testability.

## E3.02 — Database Foundation
- [ ] Database initialization.
- [ ] Versioning.
- [ ] Transaction strategy.
- [ ] Migration framework.

## E3.03 — Entity Tables
- [ ] Store.
- [ ] Orders.
- [ ] Products.
- [ ] Variations.
- [ ] Attributes.
- [ ] Sync metadata.

## E3.04 — DAO/Data Sources
- [ ] CRUD.
- [ ] Queries.
- [ ] Search support.
- [ ] Pagination/local ordering where needed.

## E3.05 — Local Repositories
- [ ] Order local repository.
- [ ] Product local repository.
- [ ] Store local repository.
- [ ] Sync queue repository.

## E3.06 — Pending Queue Persistence
- [ ] Pending state.
- [ ] Running state.
- [ ] Succeeded state.
- [ ] Failed state.
- [ ] Conflict state.
- [ ] Restart recovery.

## E3.07 — Migration & Recovery
- [ ] First migration.
- [ ] Upgrade migration.
- [ ] Rollback/error handling policy.
- [ ] Process restart test.

## E3.08 — Local Data Tests
- [ ] CRUD.
- [ ] Transactions.
- [ ] Queue persistence.
- [ ] Restart.
- [ ] Migration.
- [ ] Invalid state.

## E3.09 — Local Data Gate
- [ ] No data loss in tested migration paths.
- [ ] Queue survives restart.
- [ ] Tests green.

---

# E4 — Secure Storage

## E4.01 — Credential Boundary
- [ ] Define credential reference model.
- [ ] Keep secrets out of Domain entities.

## E4.02 — Secure Storage Implementation
- [ ] Android Keystore-backed storage.
- [ ] Store key/secret securely.
- [ ] Retrieve only when needed.

## E4.03 — Disconnect Cleanup
- [ ] Remove credential.
- [ ] Remove/retain store data according to policy.
- [ ] Clear active connection state.

## E4.04 — Backup/Logging Security
- [ ] Backup exclusion strategy.
- [ ] Log redaction.
- [ ] Crash diagnostic redaction.

## E4.05 — Security Tests
- [ ] Secret not in normal DB.
- [ ] Secret not in logs.
- [ ] Disconnect removes secret.

---

# E5 — Network Foundation

## E5.01 — HTTP Client
- [ ] HTTP engine.
- [ ] Serialization.
- [ ] Request/response pipeline.

## E5.02 — Authentication
- [ ] WooCommerce credential injection.
- [ ] HTTPS enforcement.
- [ ] Header policy.

## E5.03 — Request Policies
- [ ] Timeout.
- [ ] Pagination.
- [ ] Retry classification.
- [ ] Cancellation.

## E5.04 — Error Mapping
- [ ] 2xx parsing.
- [ ] 4xx mapping.
- [ ] 5xx mapping.
- [ ] Timeout.
- [ ] Network failure.
- [ ] Malformed response.
- [ ] Rate-limit handling.

## E5.05 — Network Tests
- [ ] Mock server.
- [ ] Auth failure.
- [ ] Timeout.
- [ ] 4xx.
- [ ] 5xx.
- [ ] Malformed payload.

---

# E6 — WooCommerce API

## E6.01 — Connection API
- [ ] Store validation.
- [ ] Connection test.
- [ ] Server/version capability check as needed.

## E6.02 — Orders Read
- [ ] List.
- [ ] Detail.
- [ ] Search.
- [ ] Filter.
- [ ] Sort/pagination.

## E6.03 — Orders Write
- [ ] Edit order.
- [ ] Status update.
- [ ] Notes.
- [ ] Shipping.
- [ ] Payment-related fields in V1 scope.
- [ ] Cancel/delete according to V1 decision.

## E6.04 — Products Read
- [ ] List.
- [ ] Search.
- [ ] Detail.

## E6.05 — Products Write
- [ ] Create.
- [ ] Edit.
- [ ] Delete.
- [ ] Simple product.
- [ ] Variable product.

## E6.06 — Variations
- [ ] List.
- [ ] Create.
- [ ] Edit.
- [ ] Delete.

## E6.07 — Attributes
- [ ] Global attributes.
- [ ] Custom attributes.
- [ ] Values/terms as required.

## E6.08 — Images
- [ ] Main image.
- [ ] Gallery.
- [ ] Upload/update/delete behavior according to API capability and V1 decision.

## E6.09 — API Integration Tests
- [ ] Orders.
- [ ] Products.
- [ ] Variations.
- [ ] Attributes.
- [ ] Images.
- [ ] Error paths.

## E6.10 — API Gate
- [ ] All V1 API operations mapped.
- [ ] Error mapping complete.
- [ ] Integration tests green.

---

# E7 — Repository & Data Orchestration

## E7.01 — Remote Data Sources
- [ ] Order remote source.
- [ ] Product remote source.
- [ ] Store remote source.

## E7.02 — Mapping
- [ ] DTO → Domain.
- [ ] Domain → request DTO.
- [ ] Null/default handling.
- [ ] Server field preservation.

## E7.03 — Repository Implementations
- [ ] Read strategy.
- [ ] Local cache.
- [ ] Remote refresh.
- [ ] Write strategy.

## E7.04 — Local-first Mutation
- [ ] Local transaction.
- [ ] Pending operation creation.
- [ ] UI-visible local state.

## E7.05 — Repository Tests
- [ ] Local hit.
- [ ] Remote refresh.
- [ ] Remote failure.
- [ ] Mutation enqueue.

---

# E8 — Sync Engine

## E8.01 — Queue Manager
- [ ] Queue retrieval.
- [ ] Ordering.
- [ ] Lock/claim.
- [ ] State transitions.

## E8.02 — Sync Worker
- [ ] Execute operation.
- [ ] Success handling.
- [ ] Failure handling.
- [ ] Persistence.

## E8.03 — Retry & Backoff
- [ ] Retry classification.
- [ ] Backoff.
- [ ] Maximum attempts policy.
- [ ] Permanent failure state.

## E8.04 — Idempotency
- [ ] Operation identity.
- [ ] Duplicate prevention.
- [ ] Safe retry behavior.

## E8.05 — Version Check
- [ ] Read server version.
- [ ] Compare `date_modified_gmt`.
- [ ] Detect stale local state.

## E8.06 — Conflict Resolution
- [ ] Safe merge rules.
- [ ] Non-mergeable conflict.
- [ ] User resolution contract.
- [ ] Conflict persistence.

## E8.07 — Recovery
- [ ] App restart.
- [ ] Process death.
- [ ] Network loss.
- [ ] Server unavailable.
- [ ] Queue recovery.

## E8.08 — Sync State Exposure
- [ ] Pending.
- [ ] Syncing.
- [ ] Synced.
- [ ] Failed.
- [ ] Conflict.

## E8.09 — Sync Test Matrix
- [ ] Offline mutation.
- [ ] Reconnect.
- [ ] Retry.
- [ ] Duplicate request.
- [ ] Conflict.
- [ ] Restart.
- [ ] Large queue.

## E8.10 — Sync Gate
- [ ] No silent data loss.
- [ ] No silent overwrite of newer server data.
- [ ] Duplicate-safe.
- [ ] Recovery tested.

---

# E9 — Background & Notifications

## E9.01 — WorkManager Foundation
- [ ] Worker.
- [ ] Constraints.
- [ ] Scheduling.
- [ ] Cancellation/retry behavior.

## E9.02 — Periodic Order Detection
- [ ] Poll WooCommerce.
- [ ] Determine new orders.
- [ ] Persist last observed state.

## E9.03 — Deduplication
- [ ] Order identity.
- [ ] Already-notified state.
- [ ] Restart-safe deduplication.

## E9.04 — Notification Provider
- [ ] Notification channel.
- [ ] Payload.
- [ ] Title/body.
- [ ] Order number.
- [ ] Amount.
- [ ] Item summary.
- [ ] Date/time.

## E9.05 — Deep Link
- [ ] Notification tap.
- [ ] Order lookup.
- [ ] Offline fallback.

## E9.06 — Background Tests
- [ ] App open.
- [ ] App closed.
- [ ] Restart.
- [ ] Network unavailable.
- [ ] Duplicate detection.
- [ ] Android scheduling constraints.

## E9.07 — Notification Gate
- [ ] No permanent connection.
- [ ] No duplicate notification.
- [ ] Tap reliably reaches order.

---

# E10 — Presentation Foundation

## E10.01 — App State Architecture
- [ ] Global app state.
- [ ] Navigation state.
- [ ] Connection state.
- [ ] Sync state.

## E10.02 — Feature State Pattern
- [ ] UI State.
- [ ] User Intent/Action.
- [ ] ViewModel/state holder.
- [ ] One-way data flow.

## E10.03 — Common States
- [ ] Loading.
- [ ] Empty.
- [ ] Error.
- [ ] Offline.
- [ ] Pending.
- [ ] Synced.
- [ ] Failed.
- [ ] Conflict.

## E10.04 — Navigation
- [ ] Route definitions.
- [ ] Arguments.
- [ ] Notification deep links.
- [ ] Back behavior.

---

# E11 — Design System & UI

## E11.01 — Theme Foundation
- [ ] RTL-first.
- [ ] LTR-ready.
- [ ] Typography.
- [ ] Colors.
- [ ] Spacing.
- [ ] Shapes.

## E11.02 — Liquid Glass Components
- [ ] Surface.
- [ ] Card.
- [ ] Button.
- [ ] Input.
- [ ] Bottom/top elements.
- [ ] Dialog.
- [ ] Status indicators.

## E11.03 — Connection/Onboarding
- [ ] Store URL.
- [ ] Consumer Key.
- [ ] Consumer Secret.
- [ ] Connection test.
- [ ] Error states.

## E11.04 — Dashboard
- [ ] New orders count.
- [ ] Operational summary.
- [ ] Sync/connection status.
- [ ] Navigation.

## E11.05 — Orders
- [ ] List.
- [ ] Search/filter.
- [ ] Order detail.
- [ ] Quick actions.
- [ ] Full edit.
- [ ] Status.
- [ ] Notes.
- [ ] Conflict state.

## E11.06 — Products
- [ ] List.
- [ ] Search.
- [ ] Quick add.
- [ ] Full edit.
- [ ] Delete.

## E11.07 — Variable Products
- [ ] Variable product.
- [ ] Attributes.
- [ ] Variations.
- [ ] Variation edit.

## E11.08 — Images
- [ ] Main image.
- [ ] Gallery.
- [ ] Loading/error states.

## E11.09 — Settings
- [ ] Connection state.
- [ ] Sync state.
- [ ] Disconnect.
- [ ] Security-sensitive actions.

## E11.10 — UI Accessibility & State Review
- [ ] RTL correctness.
- [ ] Text scaling.
- [ ] Touch targets.
- [ ] Loading/empty/error coverage.

---

# E12 — UI ↔ Core Integration

## E12.01 — Connection Flow
- [ ] UI → UseCase → SecureStorage/API → Local Store.

## E12.02 — Orders Flow
- [ ] List.
- [ ] Detail.
- [ ] Edit.
- [ ] Local mutation.
- [ ] Sync.

## E12.03 — Products Flow
- [ ] List.
- [ ] Add.
- [ ] Edit.
- [ ] Delete.
- [ ] Variable/Variation.
- [ ] Attributes.
- [ ] Images.

## E12.04 — Sync UI
- [ ] Status Dot.
- [ ] Pending.
- [ ] Failed.
- [ ] Conflict.
- [ ] Retry action.

## E12.05 — Notification Flow
- [ ] Notification → Deep Link → Order.
- [ ] Missing/offline order behavior.

## E12.06 — Integration Gate
- [ ] No business logic duplicated in UI.
- [ ] No direct API calls from Compose.
- [ ] All critical flows end-to-end tested.

---

# E13 — Full Test & Resilience

## E13.01 — Unit Suite
- [ ] Domain.
- [ ] Repositories.
- [ ] Queue.
- [ ] Sync.
- [ ] Mapping.

## E13.02 — Integration Suite
- [ ] Database.
- [ ] API.
- [ ] Repository.
- [ ] Worker.
- [ ] Notifications.

## E13.03 — UI Suite
- [ ] Connection.
- [ ] Dashboard.
- [ ] Orders.
- [ ] Products.
- [ ] Variations.
- [ ] Settings.

## E13.04 — Network Resilience
- [ ] Offline.
- [ ] Slow.
- [ ] Drop.
- [ ] Timeout.
- [ ] 4xx.
- [ ] 5xx.
- [ ] Rate limit.

## E13.05 — Lifecycle Resilience
- [ ] Process death.
- [ ] Restart.
- [ ] Update.
- [ ] Migration.
- [ ] Background restriction.

## E13.06 — Data Integrity
- [ ] No duplicate mutation.
- [ ] No silent overwrite.
- [ ] No silent data loss.
- [ ] Conflict preserved.

## E13.07 — Performance
- [ ] Startup.
- [ ] Memory.
- [ ] Large catalog.
- [ ] Large order.
- [ ] Large image.
- [ ] Large queue.

---

# E14 — Beta & Hardening

## E14.01 — Security Audit
- [ ] Credentials.
- [ ] Logs.
- [ ] Backup.
- [ ] Network.
- [ ] Crash data.

## E14.02 — Performance Audit
- [ ] Startup.
- [ ] Memory.
- [ ] Battery.
- [ ] Network usage.

## E14.03 — Real WooCommerce Validation
- [ ] Real store connection.
- [ ] Real orders.
- [ ] Real products.
- [ ] Variable products.
- [ ] Images.
- [ ] Slow hosting.

## E14.04 — UX Audit
- [ ] Critical flows.
- [ ] Error recovery.
- [ ] Offline UX.
- [ ] Sync visibility.
- [ ] Conflict UX.

## E14.05 — Bug Triage
- [ ] Blockers = 0.
- [ ] Critical = 0.
- [ ] High severity reviewed.
- [ ] Known limitations documented.

---

# E15 — V1 Release

## E15.01 — Scope Freeze
- [ ] Compare with Product Vision.
- [ ] Compare with Roadmap.
- [ ] Compare with Decision Tracker.
- [ ] Verify no accidental Future feature.

## E15.02 — Final Regression
- [ ] Full test suite green.
- [ ] Critical manual flows green.
- [ ] Migration verified.

## E15.03 — Release Build
- [ ] Version.
- [ ] Signing.
- [ ] Release configuration.
- [ ] Reproducible build.

## E15.04 — Documentation
- [ ] README status.
- [ ] Architecture status.
- [ ] Known limitations.
- [ ] Release notes.
- [ ] Build instructions.

## E15.05 — GitHub Release
- [ ] V1 tag.
- [ ] Release notes.
- [ ] Source state verified.

## E15.06 — Final V1 Gate
- [ ] New order notification.
- [ ] Order management.
- [ ] Product management.
- [ ] Local-first.
- [ ] Sync/retry/conflict.
- [ ] Secure credentials.
- [ ] Resilience matrix.
- [ ] No blocker/critical issue.

---

# Dependency Chain

```text
E0 Foundation
  ↓
E1 Core Foundation
  ↓
E2 Domain Models
  ↓
E3 Local Data ─────┐
  ↓                │
E4 Secure Storage  │
  ↓                │
E5 Network ────────┤
  ↓                │
E6 WooCommerce API │
  ↓                │
E7 Repositories ───┘
  ↓
E8 Sync Engine
  ↓
E9 Background + Notifications
  ↓
E10 Presentation Foundation
  ↓
E11 Design + UI
  ↓
E12 Integration
  ↓
E13 Full Test
  ↓
E14 Beta/Hardening
  ↓
E15 Release
```

## موازی‌سازی مجاز

پس از تثبیت قراردادهای Core، بعضی کارها می‌توانند موازی شوند، اما بدون شکستن Dependency:

- E3 Local Data و E5 Network Foundation می‌توانند بعد از E2 موازی پیش بروند.
- E4 Security می‌تواند هم‌زمان با E3/E5 پیش برود.
- Design System اولیه می‌تواند قبل از کامل شدن API طراحی شود، اما Screen integration نباید قبل از Core contracts انجام شود.
- تست‌ها همیشه همراه همان مرحله نوشته می‌شوند.

## قانون اصلی اجرا

**هر Task که به `[x]` می‌رود باید سه چیز داشته باشد:**

1. Implementation/Artifact واقعی.
2. Review متناسب با Task.
3. Test یا دلیل مستند برای اینکه چرا Test در آن Task کاربرد ندارد.

تا وقتی این سه مورد وجود نداشته باشد، Task Done نیست.
