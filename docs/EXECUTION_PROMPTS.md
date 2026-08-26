# WooGit V1 — پرامپت اجرایی هر Task

این سند برای اجرای مرحله‌به‌مرحله `docs/EXECUTION_PLAN.md` ساخته شده است. هر Prompt باید فقط Task مربوط به خودش را انجام دهد، Repository را قبل از تغییر بررسی کند، Dependencyها را رعایت کند و پس از اجرا نتیجه، تست‌ها و فایل‌های تغییرکرده را گزارش کند.

## Prompt مشترک
> ابتدا Repository ووگیت را کامل برای Task مشخص‌شده بررسی کن. قبل از هر تغییر، `README.md`، `docs/ARCHITECTURE.md`، `docs/PROJECT_MAP.md`، `docs/BUILD_PLAN.md`، `docs/BUILD_REQUIREMENTS.md`، `docs/BUILD_CHECKLIST.md`، `docs/EXECUTION_PLAN.md` و `docs/DECISION_TRACKER.md` را در حد مرتبط بخوان. تصمیم‌های قبلی را نقض نکن. Scope را گسترش نده. فقط Task تعیین‌شده و dependencyهای ضروری آن را پیاده‌سازی کن. هیچ Future feature را فعال نکن. پس از تغییر، build/test/review لازم را اجرا کن. اگر چیزی مانع است، حدس نزن و آن را Blocker اعلام کن. در پایان دقیقاً گزارش بده: چه ساخته شد، چه فایل‌هایی تغییر کردند، چه تست‌هایی اجرا شدند، نتیجه تست‌ها چه بود، و آیا Task واقعاً Done است یا نه.

---

# E0 — Foundation

### E0.01 — Project Skeleton
> Prompt: E0.01 را اجرا کن. Android/KMP project skeleton را مطابق معماری ووگیت ایجاد کن. package root و identifiers را تعیین کن، Gradle sync و اولین build را اجرا کن. هنوز هیچ feature محصولی نساز.

### E0.02 — Module Boundaries
> Prompt: E0.02 را اجرا کن. Moduleهای Core/Domain، Data/Infrastructure، Presentation/UI و App/Android را با dependency direction مشخص ایجاد کن. Core نباید به Android یا Compose وابسته شود. dependency graph را بررسی و مستند کن.

### E0.03 — Kotlin/KMP/Compose
> Prompt: E0.03 را اجرا کن. Kotlin، KMP source sets، Compose، SDK و Java/Kotlin toolchain را طبق تصمیم‌های پروژه تنظیم کن. فقط configuration لازم برای V1 را اضافه کن و build را اجرا کن.

### E0.04 — Dependency Management
> Prompt: E0.04 را اجرا کن. version catalog یا convention مناسب ایجاد کن، dependencyهای لازم را تعیین کن و از اضافه‌کردن کتابخانه بدون نیاز معماری جلوگیری کن. dependency graph را بررسی کن.

### E0.05 — Quality Tooling
> Prompt: E0.05 را اجرا کن. formatting، lint/static analysis و naming/package rules را تنظیم کن. quality checks را اجرا و مشکلات واقعی را اصلاح کن، بدون تغییر scope.

### E0.06 — Test Infrastructure
> Prompt: E0.06 را اجرا کن. infrastructure تست، shared test utilities، fake/test data strategy و naming convention را ایجاد کن و حداقل یک تست واقعی سبز اضافه کن.

### E0.07 — CI Foundation
> Prompt: E0.07 را اجرا کن. GitHub Actions برای build، unit tests و quality checks بساز. workflow را تا حد امکان با همان build محلی همسان کن و failureها را قابل مشاهده کن.

### E0.08 — Foundation Gate
> Prompt: E0.08 را اجرا کن. کل E0 را review کن؛ architecture، dependency، build، test و CI را بررسی کن. فقط اگر همه معیارها پاس شدند Gate را Done کن؛ در غیر این صورت Blockerها را دقیق ثبت کن.

---

# E1 — Core Foundation

### E1.01 — Core Package Structure
> Prompt: E1.01 را اجرا کن. ساختار packageهای Domain شامل entity، usecase، repository، sync، error و event را ایجاد کن. هیچ business implementation اضافی نساز.

### E1.02 — Identity & Time
> Prompt: E1.02 را اجرا کن. strategy پایدار برای Entity ID، Store ID، timestamp و UTC ایجاد کن. primitiveها باید platform-independent باشند و تست شوند.

### E1.03 — Result & Domain Error
> Prompt: E1.03 را اجرا کن. typed Domain Error و Result strategy بساز و recoverable/non-recoverable را مشخص کن. mapping موردنیاز Presentation را به شکل contract تعریف کن.

### E1.04 — Version Provider
> Prompt: E1.04 را اجرا کن. Version abstraction ایجاد کن و V1 را بر اساس `date_modified_gmt` طراحی کن. Future Commit provider فقط به‌صورت boundary باقی بماند.

### E1.05 — Repository Contracts
> Prompt: E1.05 را اجرا کن. قرارداد Repositoryهای Order، Product، Store و Sync/Pending را تعریف کن. قراردادها باید Domain-owned و مستقل از API/Database باشند.

### E1.06 — Use Cases
> Prompt: E1.06 را اجرا کن. Use Case contractهای Connection، Orders، Products و Sync را تعریف کن. business rule را داخل UI یا API قرار نده.

### E1.07 — Events/Notifications
> Prompt: E1.07 را اجرا کن. Event model و boundaryهای publisher/subscriber و Notification intent/provider را تعریف کن؛ provider واقعی فعلاً نساز.

### E1.08 — Future Boundaries
> Prompt: E1.08 را اجرا کن. Store scope و boundaryهای Multi-Store و AI/Assistant را برای آینده آماده کن، بدون فعال‌کردن قابلیت‌های Future در V1.

### E1.09 — Core Tests
> Prompt: E1.09 را اجرا کن. برای ID/time، error، version و contractهای قابل تست Unit Test بنویس. تست‌ها باید deterministic باشند.

### E1.10 — Core Gate
> Prompt: E1.10 را اجرا کن. Core را از نظر dependency، Android/UI leakage، version abstraction و Future boundaries بررسی کن و کل تست‌های Core را اجرا کن.

---

# E2 — Domain Models

### E2.01 — Store Connection
> Prompt: E2.01 را اجرا کن. StoreConnection domain model را با identity، URL، connection state و credential reference بساز. raw secret را وارد domain persistence نکن.

### E2.02 — Orders
> Prompt: E2.02 را اجرا کن. مدل‌های Order، OrderItem، Customer، Address، Payment، Shipping، Discount، Notes و Status را طبق API و نیازمندی V1 ایجاد کن. قبل از پیاده‌سازی mappingها را با WooCommerce contract تطبیق بده.

### E2.03 — Products
> Prompt: E2.03 را اجرا کن. Product، ProductImage، Gallery، Pricing و stock fields لازم V1 را ایجاد کن و UI-specific field وارد Domain نکن.

### E2.04 — Variable Products
> Prompt: E2.04 را اجرا کن. Variation و variation attributes و pricing/stock fields را ایجاد کن و رابطه Product/Variation را تست کن.

### E2.05 — Attributes
> Prompt: E2.05 را اجرا کن. Global/Custom Attribute و term/value representation را مطابق WooCommerce API ایجاد و validate کن.

### E2.06 — Sync Models
> Prompt: E2.06 را اجرا کن. SyncState، PendingOperation، OperationType، RetryMetadata، EntityVersion و Conflict را طراحی کن. state transitionهای معتبر را مشخص کن.

### E2.07 — Validation Tests
> Prompt: E2.07 را اجرا کن. برای مدل‌های Domain تست valid/invalid، timestamp و version بنویس و mapping assumptions را ثبت کن.

### E2.08 — Domain Gate
> Prompt: E2.08 را اجرا کن. مدل‌ها را با API و Requirements تطبیق بده، ownership را بررسی کن و مطمئن شو UI-specific model leakage نداریم.

---

# E3 — Local Data

### E3.01 — Database Strategy
> Prompt: E3.01 را اجرا کن. گزینه‌های database سازگار با معماری V1/KMP را بررسی کن و بر اساس transaction، migration، testability و maintenance یک گزینه را انتخاب کن. قبل از تصمیم، مستندات/نسخه‌های فعلی ابزار را بررسی کن و تصمیم را ثبت کن.

### E3.02 — Database Foundation
> Prompt: E3.02 را اجرا کن. database initialization، versioning، transaction strategy و migration framework انتخاب‌شده را پیاده کن و یک smoke test بساز.

### E3.03 — Entity Tables
> Prompt: E3.03 را اجرا کن. schema مربوط به Store، Orders، Products، Variations، Attributes و sync metadata را بساز. indexهای موردنیاز queryهای V1 را در نظر بگیر.

### E3.04 — DAO/Data Sources
> Prompt: E3.04 را اجرا کن. CRUD، query، search و pagination/local ordering لازم را پیاده کن و queryهای سنگین را بررسی کن.

### E3.05 — Local Repositories
> Prompt: E3.05 را اجرا کن. Repositoryهای Local برای Order، Product، Store و Queue را بر اساس Core contracts پیاده کن.

### E3.06 — Queue Persistence
> Prompt: E3.06 را اجرا کن. persistence کامل queue stateها را بساز و مطمئن شو queue بعد از process restart قابل recovery است.

### E3.07 — Migration/Recovery
> Prompt: E3.07 را اجرا کن. migration اولیه و upgrade path را ایجاد و process restart/recovery را تست کن. data loss نباید رخ دهد.

### E3.08 — Local Tests
> Prompt: E3.08 را اجرا کن. CRUD، transaction، queue persistence، restart، migration و invalid-state tests را کامل کن.

### E3.09 — Local Gate
> Prompt: E3.09 را اجرا کن. Local Data را review کن و فقط در صورت اثبات migration safety، queue persistence و تست سبز Gate را پاس کن.

---

# E4 — Secure Storage

### E4.01 — Credential Boundary
> Prompt: E4.01 را اجرا کن. credential reference را از raw secret جدا کن و مطمئن شو Domain و DB معمولی secret را نگه نمی‌دارند.

### E4.02 — Secure Storage
> Prompt: E4.02 را اجرا کن. Android Keystore-backed secure storage را مطابق platform security guidance پیاده کن و credential را فقط هنگام نیاز بازیابی کن.

### E4.03 — Disconnect Cleanup
> Prompt: E4.03 را اجرا کن. disconnect را طوری پیاده کن که credential حذف شود و store state/data طبق policy پروژه مدیریت شود.

### E4.04 — Backup/Logging
> Prompt: E4.04 را اجرا کن. backup exclusion، log redaction و crash diagnostic redaction را بررسی و پیاده کن.

### E4.05 — Security Tests
> Prompt: E4.05 را اجرا کن. اثبات کن secret در DB/log وجود ندارد و disconnect آن را حذف می‌کند.

---

# E5 — Network Foundation

### E5.01 — HTTP Client
> Prompt: E5.01 را اجرا کن. HTTP client، serialization و request/response pipeline را با کمترین abstraction لازم پیاده کن و API-specific logic را در Network Foundation نریز.

### E5.02 — Authentication
> Prompt: E5.02 را اجرا کن. WooCommerce credential injection، HTTPS enforcement و header policy را پیاده کن. secret را log نکن.

### E5.03 — Request Policies
> Prompt: E5.03 را اجرا کن. timeout، pagination، retry classification و cancellation را در سطح مناسب پیاده کن.

### E5.04 — Error Mapping
> Prompt: E5.04 را اجرا کن. 2xx، 4xx، 5xx، timeout، network failure، malformed response و rate limit را به typed errors نگاشت کن.

### E5.05 — Network Tests
> Prompt: E5.05 را اجرا کن. Mock server/test doubles برای auth failure، timeout، 4xx، 5xx و malformed payload بساز و تست‌ها را اجرا کن.

---

# E6 — WooCommerce API

### E6.01 — Connection API
> Prompt: E6.01 را اجرا کن. store validation و connection test را بر اساس WooCommerce REST API پیاده کن. capability/version check فقط اگر برای V1 واقعاً لازم است اضافه شود.

### E6.02 — Orders Read
> Prompt: E6.02 را اجرا کن. Orders list/detail/search/filter/sort/pagination را با API client و repository contracts یکپارچه کن.

### E6.03 — Orders Write
> Prompt: E6.03 را اجرا کن. edit/status/notes/shipping/payment-related fields و cancel/delete فقط طبق V1 Decision Tracker را پیاده کن. mutationها را retry-safe طراحی کن.

### E6.04 — Products Read
> Prompt: E6.04 را اجرا کن. Product list/search/detail را پیاده و mapping را تست کن.

### E6.05 — Products Write
> Prompt: E6.05 را اجرا کن. create/edit/delete و simple/variable product operations را پیاده کن.

### E6.06 — Variations
> Prompt: E6.06 را اجرا کن. variation list/create/edit/delete را پیاده کن و parent product consistency را حفظ کن.

### E6.07 — Attributes
> Prompt: E6.07 را اجرا کن. global/custom attributes و values/terms لازم V1 را پیاده کن.

### E6.08 — Images
> Prompt: E6.08 را اجرا کن. main image/gallery operations را مطابق قابلیت واقعی WooCommerce API و تصمیم V1 پیاده کن. محدودیت حجم/فرمت و خطاها را مدیریت کن.

### E6.09 — API Integration Tests
> Prompt: E6.09 را اجرا کن. integration testهای Orders، Products، Variations، Attributes، Images و error paths را اضافه کن.

### E6.10 — API Gate
> Prompt: E6.10 را اجرا کن. همه عملیات V1 API را با Requirements تطبیق بده و integration tests را کامل اجرا کن.

---

# E7 — Repository/Data Orchestration

### E7.01 — Remote Sources
> Prompt: E7.01 را اجرا کن. Remote DataSourceهای Order/Product/Store را بر اساس API contracts پیاده کن.

### E7.02 — Mapping
> Prompt: E7.02 را اجرا کن. DTO↔Domain mapping را ایجاد کن و null/default/server-field preservation را پوشش بده.

### E7.03 — Repository Implementations
> Prompt: E7.03 را اجرا کن. repository implementationها را با local cache، remote refresh و write strategy کامل کن.

### E7.04 — Local-first Mutation
> Prompt: E7.04 را اجرا کن. mutation را local-first پیاده کن: transaction → pending operation → observable local state. تا قبل از persistence موفق، operation گم نشود.

### E7.05 — Repository Tests
> Prompt: E7.05 را اجرا کن. local hit، remote refresh، remote failure و mutation enqueue را تست کن.

---

# E8 — Sync Engine

### E8.01 — Queue Manager
> Prompt: E8.01 را اجرا کن. queue retrieval، deterministic ordering، lock/claim و state transitionها را پیاده کن. race conditionها را بررسی کن.

### E8.02 — Sync Worker
> Prompt: E8.02 را اجرا کن. worker را برای اجرای operation و ثبت success/failure بساز. هیچ failure را silently swallow نکن.

### E8.03 — Retry/Backoff
> Prompt: E8.03 را اجرا کن. retry classification، backoff، maximum attempts و permanent failure state را پیاده کن.

### E8.04 — Idempotency
> Prompt: E8.04 را اجرا کن. operation identity و duplicate prevention را طراحی و تست کن تا retry باعث duplicate mutation نشود.

### E8.05 — Version Check
> Prompt: E8.05 را اجرا کن. server `date_modified_gmt` را با local version مقایسه کن و stale local state را تشخیص بده.

### E8.06 — Conflict Resolution
> Prompt: E8.06 را اجرا کن. safe merge، non-mergeable conflict، user resolution contract و conflict persistence را پیاده کن. هیچ overwrite خاموشی مجاز نیست.

### E8.07 — Recovery
> Prompt: E8.07 را اجرا کن. restart، process death، network loss، server unavailable و queue recovery را تست و اصلاح کن.

### E8.08 — Sync State Exposure
> Prompt: E8.08 را اجرا کن. stateهای pending/syncing/synced/failed/conflict را به presentation contract قابل مشاهده تبدیل کن.

### E8.09 — Sync Test Matrix
> Prompt: E8.09 را اجرا کن. offline mutation، reconnect، retry، duplicate، conflict، restart و large queue را تست کن.

### E8.10 — Sync Gate
> Prompt: E8.10 را اجرا کن. با تست و evidence ثابت کن silent data loss، silent overwrite و duplicate mutation نداریم. در صورت شکست حتی یک سناریوی critical، Gate را Blocked نگه دار.

---

# E9 — Background & Notifications

### E9.01 — WorkManager
> Prompt: E9.01 را اجرا کن. WorkManager worker، constraints، scheduling و cancellation/retry را بر اساس Android guidance پیاده کن. اتصال دائمی نساز.

### E9.02 — Order Detection
> Prompt: E9.02 را اجرا کن. periodic polling و new-order detection را با local state پیاده کن.

### E9.03 — Deduplication
> Prompt: E9.03 را اجرا کن. notification deduplication را با order identity و persisted state پیاده کن تا restart هم duplicate نسازد.

### E9.04 — Notification Provider
> Prompt: E9.04 را اجرا کن. notification channel و payload سفارش جدید را بساز؛ فقط داده لازم را نشان بده.

### E9.05 — Deep Link
> Prompt: E9.05 را اجرا کن. notification tap را به order detail متصل کن و offline/missing-order behavior را مشخص کن.

### E9.06 — Background Tests
> Prompt: E9.06 را روی سناریوهای app open/closed/restart/network unavailable و Android scheduling اجرا کن.

### E9.07 — Notification Gate
> Prompt: E9.07 را بررسی کن. مطمئن شو permanent connection وجود ندارد، duplicate notification رخ نمی‌دهد و deep link قابل اعتماد است.

---

# E10 — Presentation Foundation

### E10.01 — App State
> Prompt: E10.01 را اجرا کن. global app state، navigation state، connection state و sync state را بدون قرار دادن business logic در UI تعریف کن.

### E10.02 — Feature State
> Prompt: E10.02 را اجرا کن. UI State، user intent، ViewModel/state holder و one-way data flow را برای featureها تعریف کن.

### E10.03 — Common States
> Prompt: E10.03 را اجرا کن. loading/empty/error/offline/pending/synced/failed/conflict را به الگوی مشترک تبدیل کن.

### E10.04 — Navigation
> Prompt: E10.04 را اجرا کن. routes، arguments، notification deep links و back behavior را پیاده کن.

---

# E11 — Design System & UI

### E11.01 — Theme
> Prompt: E11.01 را اجرا کن. RTL-first/LTR-ready theme، typography، colors، spacing و shapes را مطابق طراحی پروژه بساز.

### E11.02 — Liquid Glass Components
> Prompt: E11.02 را اجرا کن. componentهای Liquid Glass را با API قابل استفاده مجدد بساز و accessibility/contrast را بررسی کن.

### E11.03 — Connection UI
> Prompt: E11.03 را اجرا کن. onboarding/connection screen را با URL/key/secret، test connection و error state بساز. secret را در state غیرضروری نگه ندار.

### E11.04 — Dashboard
> Prompt: E11.04 را اجرا کن. dashboard را با new orders، summary، connection/sync status و navigation بساز.

### E11.05 — Orders UI
> Prompt: E11.05 را اجرا کن. order list/detail/quick actions/full edit/status/notes/conflict را با local-first state بساز.

### E11.06 — Products UI
> Prompt: E11.06 را اجرا کن. product list/search/quick add/full edit/delete را پیاده کن.

### E11.07 — Variable Products UI
> Prompt: E11.07 را اجرا کن. variable product، attributes، variations و variation edit را بساز.

### E11.08 — Images UI
> Prompt: E11.08 را اجرا کن. main image/gallery و loading/error stateها را بساز و handling تصویر بزرگ را در UI لحاظ کن.

### E11.09 — Settings
> Prompt: E11.09 را اجرا کن. connection/sync/disconnect و security-sensitive actions را بساز.

### E11.10 — Accessibility/State Review
> Prompt: E11.10 را اجرا کن. RTL، text scaling، touch target و همه loading/empty/error stateها را روی UI مرور کن.

---

# E12 — UI/Core Integration

### E12.01 — Connection Flow
> Prompt: E12.01 را اجرا کن. مسیر UI→UseCase→SecureStorage/API→Local Store را end-to-end وصل کن و failureها را به UI برگردان.

### E12.02 — Orders Flow
> Prompt: E12.02 را اجرا کن. orders list/detail/edit را به local-first repository و sync وصل کن.

### E12.03 — Products Flow
> Prompt: E12.03 را اجرا کن. product create/edit/delete، variable/variation، attributes و images را end-to-end وصل کن.

### E12.04 — Sync UI
> Prompt: E12.04 را اجرا کن. pending/failed/conflict/retry را در UI قابل مشاهده و actionable کن.

### E12.05 — Notification Flow
> Prompt: E12.05 را اجرا کن. notification→deep link→order detail را کامل کن و missing/offline behavior را پوشش بده.

### E12.06 — Integration Gate
> Prompt: E12.06 را اجرا کن. بررسی کن هیچ API call یا business logic مستقیمی در Compose نباشد و critical flowها end-to-end تست شوند.

---

# E13 — Full Test & Resilience

### E13.01 — Unit Suite
> Prompt: E13.01 را اجرا کن. unit suite مربوط به Domain، Repository، Queue، Sync و Mapping را کامل کن و coverage را فقط به‌عنوان metric فرعی استفاده کن؛ تمرکز روی رفتارهای critical باشد.

### E13.02 — Integration Suite
> Prompt: E13.02 را اجرا کن. database، API، repository، worker و notification integration tests را کامل کن.

### E13.03 — UI Suite
> Prompt: E13.03 را اجرا کن. connection، dashboard، orders، products، variations و settings critical UI flows را تست کن.

### E13.04 — Network Resilience
> Prompt: E13.04 را اجرا کن. offline، slow، drop، timeout، 4xx، 5xx و rate-limit را تست کن.

### E13.05 — Lifecycle Resilience
> Prompt: E13.05 را اجرا کن. process death، restart، update، migration و background restriction را تست کن.

### E13.06 — Data Integrity
> Prompt: E13.06 را اجرا کن. duplicate mutation، silent overwrite، silent data loss و conflict preservation را با test evidence بررسی کن.

### E13.07 — Performance
> Prompt: E13.07 را اجرا کن. startup، memory، large catalog، large order، large image و large queue را اندازه‌گیری کن و baseline ثبت کن.

---

# E14 — Beta & Hardening

### E14.01 — Security Audit
> Prompt: E14.01 را اجرا کن. credential، logs، backup، network و crash data را audit کن و هر secret exposure را Blocker تلقی کن.

### E14.02 — Performance Audit
> Prompt: E14.02 را اجرا کن. startup، memory، battery و network usage را روی سناریوهای واقعی بررسی و regressionهای مهم را اصلاح کن.

### E14.03 — Real WooCommerce Validation
> Prompt: E14.03 را اجرا کن. با یک WooCommerce واقعی Orders/Products/Variables/Images و شرایط server ضعیف را بررسی کن. از داده واقعی حساس در logs/test artifacts استفاده نکن.

### E14.04 — UX Audit
> Prompt: E14.04 را اجرا کن. critical flows، error recovery، offline UX، sync visibility و conflict UX را بررسی و blockerهای UX را اصلاح کن.

### E14.05 — Bug Triage
> Prompt: E14.05 را اجرا کن. همه issueها را severity‌بندی کن؛ Blocker و Critical باید صفر باشند و Highها تصمیم مشخص داشته باشند.

---

# E15 — Release

### E15.01 — Scope Freeze
> Prompt: E15.01 را اجرا کن. implementation نهایی را با Product Vision، Roadmap، Decision Tracker، Build Plan و Requirements تطبیق بده و هر feature خارج از V1 را حذف یا غیرفعال کن.

### E15.02 — Final Regression
> Prompt: E15.02 را اجرا کن. کل test suite و critical manual flows را اجرا و migration را verify کن.

### E15.03 — Release Build
> Prompt: E15.03 را اجرا کن. version، signing، release configuration و reproducible release build را آماده و verify کن. secret signing را هرگز commit نکن.

### E15.04 — Documentation
> Prompt: E15.04 را اجرا کن. README، Architecture، known limitations، release notes و build instructions را با وضعیت واقعی V1 هماهنگ کن.

### E15.05 — GitHub Release
> Prompt: E15.05 را اجرا کن. tag و GitHub release V1 را فقط پس از Final Gate ایجاد کن و source state را verify کن.

### E15.06 — Final V1 Gate
> Prompt: E15.06 را اجرا کن. New Order Notification، Order Management، Product Management، Local-first، Sync/Retry/Conflict، Secure Credentials، Resilience Matrix و نبود Blocker/Critical را با evidence بررسی کن. اگر حتی یک معیار critical fail است، Release را متوقف کن.

---

# نحوه استفاده

1. ابتدا Task متناظر در `EXECUTION_PLAN.md` را بخوان.
2. Prompt همین Task را کپی کن.
3. Prompt را به Agent/Coding AI بده.
4. Agent باید قبل از تغییر Repository را بررسی کند.
5. بعد از اجرا، Review و Test انجام شود.
6. Task فقط در صورت داشتن Implementation + Review + Test به `[x]` تبدیل شود.
7. نتیجه در `BUILD_CHECKLIST.md` ثبت شود.
8. سپس Task بعدی اجرا شود.

**این فایل عمداً Promptها را به Taskهای مشخص متصل می‌کند تا Agent نتواند بدون توجه به معماری و Dependencyهای ووگیت وارد پیاده‌سازی پراکنده شود.**
