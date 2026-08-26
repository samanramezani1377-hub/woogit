# WooGit V1 — چک‌لیست بررسی و کنترل ساخت

این سند مرجع «چه چیزی را باید بررسی کنیم؟» است. هر مرحله قبل از عبور به مرحله بعد باید بررسی‌های مربوط به خود را پاس کند.

## وضعیت‌ها
- `[ ]` بررسی نشده
- `[~]` در حال بررسی
- `[x]` تأیید شده
- `[!]` مشکل/Blocker
- `[-]` خارج از محدوده V1

> این فایل قرار نیست جای Test Suite را بگیرد؛ Test Suite باید در کد وجود داشته باشد. این سند وضعیت بررسی معماری، کیفیت، وابستگی و پذیرش هر مرحله را ثبت می‌کند.

---

# P0 — Foundation

## بررسی معماری
- [ ] ساختار Repository با Core-Out منطبق است.
- [ ] جهت وابستگی Moduleها یک‌طرفه و قابل توضیح است.
- [ ] Core به Android/Compose وابستگی ندارد.
- [ ] KMP boundary منطقی است.
- [ ] هیچ قابلیت آینده‌ای به‌صورت implementation وارد V1 نشده است.

## بررسی Build
- [ ] Clean build موفق است.
- [ ] Debug build موفق است.
- [ ] Test task موفق است.
- [ ] CI همان build/test پایه را اجرا می‌کند.
- [ ] Dependencyهای اضافه و بلااستفاده بررسی شده‌اند.

## بررسی کیفیت
- [ ] Lint/format بدون blocker است.
- [ ] Logging policy اعمال شده است.
- [ ] Secret در source code وجود ندارد.

### Gate P0
- [ ] Build + Test + Architecture Review تأیید شد.

---

# P1 — Core & Domain

## بررسی مدل‌ها
- [ ] Order model با نیازهای WooCommerce سازگار است.
- [ ] Product model ساده/متغیر را پوشش می‌دهد.
- [ ] Variation model مستقل و قابل مدیریت است.
- [ ] Attribute model Custom/Global را پوشش می‌دهد.
- [ ] Store scope از ابتدا قابل تشخیص است.
- [ ] Sync state و Pending Operation دقیق هستند.

## بررسی معماری
- [ ] Repositoryها فقط Contract هستند.
- [ ] Use Caseها Business Logic را نگه می‌دارند.
- [ ] API model و Domain model بی‌دلیل یکی نشده‌اند.
- [ ] VersionProvider abstraction وجود دارد.
- [ ] V1 از `date_modified_gmt` استفاده می‌کند.
- [ ] Commit واقعی به V1 وارد نشده است.
- [ ] Conflict model تعریف شده است.
- [ ] Notification/Event boundary مستقل است.

## بررسی تست
- [ ] Domain rules تست شده‌اند.
- [ ] Error model تست شده است.
- [ ] Version comparison تست شده است.
- [ ] Queue state transitions تست شده‌اند.

### Gate P1
- [ ] Core بدون Android build/test می‌شود.
- [ ] Domain Review تأیید شد.

---

# P2 — Local Data

## بررسی Database
- [ ] Schema با Domain تطبیق دارد.
- [ ] Primary/foreign keys صحیح‌اند.
- [ ] Indexهای لازم بررسی شده‌اند.
- [ ] Transactionهای حساس مشخص‌اند.
- [ ] Migration وجود دارد و تست شده است.

## بررسی Queue
- [ ] Pending operation بعد از restart باقی می‌ماند.
- [ ] State transitionها deterministic هستند.
- [ ] عملیات شکست‌خورده حذف نمی‌شود.
- [ ] عملیات موفق حذف/نهایی می‌شود.
- [ ] Conflict state حفظ می‌شود.

## بررسی امنیت
- [ ] Credential در DB معمولی نیست.
- [ ] داده Store قابل پاک‌سازی است.
- [ ] Backup behavior بررسی شده است.

## تست
- [ ] CRUD تست شده.
- [ ] Restart تست شده.
- [ ] Migration تست شده.
- [ ] Corrupt/invalid state handling بررسی شده.

### Gate P2
- [ ] Local Data و Queue قابل بازیابی و قابل تست هستند.

---

# P3 — WooCommerce Integration

## بررسی اتصال
- [ ] URL معتبر/نامعتبر.
- [ ] Credential معتبر/نامعتبر.
- [ ] HTTPS.
- [ ] Connection timeout.
- [ ] Server unreachable.

## بررسی Orders
- [ ] List.
- [ ] Search/filter.
- [ ] Detail.
- [ ] Items.
- [ ] Customer/address.
- [ ] Payment.
- [ ] Shipping.
- [ ] Discount.
- [ ] Notes.
- [ ] Status.
- [ ] Edit.
- [ ] Cancel/delete طبق Scope.

## بررسی Products
- [ ] List.
- [ ] Search.
- [ ] Create.
- [ ] Edit.
- [ ] Delete.
- [ ] Simple product.
- [ ] Variable product.
- [ ] Variation CRUD.
- [ ] Attributes.
- [ ] Images.
- [ ] Gallery.

## بررسی API
- [ ] Pagination.
- [ ] Serialization.
- [ ] 400/401/403/404.
- [ ] 409/conflict-like responses.
- [ ] 429/rate limit.
- [ ] 5xx.
- [ ] Timeout.
- [ ] Malformed response.
- [ ] Server validation errors.

### Gate P3
- [ ] Integration tests سبز.
- [ ] هیچ endpoint حیاتی بدون error mapping نیست.

---

# P4 — Sync Engine

## بررسی جریان اصلی
- [ ] Local mutation بلافاصله قابل مشاهده است.
- [ ] Mutation وارد Queue می‌شود.
- [ ] Worker Queue را پردازش می‌کند.
- [ ] موفقیت Server state را به‌روز می‌کند.
- [ ] شکست Queue state را حفظ می‌کند.

## بررسی Retry
- [ ] Retry محدود و deterministic است.
- [ ] Backoff وجود دارد.
- [ ] Timeout کنترل شده است.
- [ ] Retry باعث duplicate mutation نمی‌شود.

## بررسی Conflict
- [ ] Server version خوانده می‌شود.
- [ ] Local/server تغییر هم‌زمان تشخیص داده می‌شود.
- [ ] Merge فقط در موارد امن انجام می‌شود.
- [ ] Conflict غیرقابل merge به کاربر منتقل می‌شود.
- [ ] overwrite خاموش وجود ندارد.

## بررسی Recovery
- [ ] Process death.
- [ ] App restart.
- [ ] Network drop.
- [ ] Server down.
- [ ] Offline طولانی.
- [ ] Queue بزرگ.

### Gate P4
- [ ] هیچ سناریوی شناخته‌شده‌ای باعث silent data loss نمی‌شود.
- [ ] Sync resilience review تأیید شد.

---

# P5 — Background Detection & Notifications

## بررسی Background
- [ ] WorkManager correctly configured.
- [ ] Periodic work محدودیت‌های Android را رعایت می‌کند.
- [ ] سرویس دائمی/Foreground بی‌دلیل استفاده نشده است.
- [ ] شرایط battery/network بررسی شده‌اند.

## بررسی Order Detection
- [ ] سفارش جدید تشخیص داده می‌شود.
- [ ] سفارش قدیمی دوباره اعلان نمی‌شود.
- [ ] تغییرات عادی با New Order اشتباه نمی‌شوند.
- [ ] duplicate notification حذف شده است.

## بررسی Notification
- [ ] عنوان صحیح.
- [ ] شماره سفارش.
- [ ] مبلغ.
- [ ] اطلاعات کالا.
- [ ] تاریخ/ساعت.
- [ ] tap action.
- [ ] deep link.
- [ ] App closed behavior.

### Gate P5
- [ ] Notification flow در دستگاه واقعی تست شده است.

---

# P6 — Security

## بررسی Credential
- [ ] Secret در Secure Storage است.
- [ ] Secret در Log نیست.
- [ ] Secret در Crash payload نیست.
- [ ] Secret در backup ناخواسته نیست.
- [ ] Disconnect credential را پاک می‌کند.

## بررسی Network
- [ ] HTTPS enforced/validated.
- [ ] certificate/TLS handling مناسب است.
- [ ] sensitive headers در error logs چاپ نمی‌شوند.

### Gate P6
- [ ] Security review بدون Critical/High blocker.

---

# P7 — UI / Design System

## بررسی Design System
- [ ] Typography.
- [ ] Spacing.
- [ ] Colors.
- [ ] Components.
- [ ] Liquid Glass rules.
- [ ] RTL.
- [ ] LTR readiness.
- [ ] Accessibility basics.

## بررسی States
- [ ] Loading.
- [ ] Empty.
- [ ] Error.
- [ ] Offline.
- [ ] Pending.
- [ ] Synced.
- [ ] Failed.
- [ ] Conflict.

## بررسی Screens
- [ ] Connection/Onboarding.
- [ ] Dashboard.
- [ ] Orders.
- [ ] Order detail.
- [ ] Order edit.
- [ ] Products.
- [ ] Quick add.
- [ ] Product edit.
- [ ] Variable product.
- [ ] Variations.
- [ ] Attributes.
- [ ] Images/gallery.
- [ ] Settings.

### Gate P7
- [ ] Hi-Fi design review تأیید شده.
- [ ] Critical screen بدون missing state نیست.

---

# P8 — UI ↔ Core

## بررسی State
- [ ] UI از Local state تغذیه می‌شود.
- [ ] Mutation از Use Case عبور می‌کند.
- [ ] Sync status قابل مشاهده است.
- [ ] Error state دقیق است.
- [ ] Conflict flow کامل است.

## بررسی معماری
- [ ] Business Logic در Compose نیست.
- [ ] API call مستقیم از UI وجود ندارد.
- [ ] ViewModelها قابل تست‌اند.
- [ ] Navigation/deep links صحیح‌اند.

### Gate P8
- [ ] Critical user journeys end-to-end کار می‌کنند.

---

# P9 — Full Test Matrix

## Functional
- [ ] Connection.
- [ ] Orders.
- [ ] Order editing.
- [ ] Products.
- [ ] Variations.
- [ ] Attributes.
- [ ] Images.
- [ ] Notifications.

## Resilience
- [ ] Offline.
- [ ] Slow network.
- [ ] Network drop.
- [ ] Timeout.
- [ ] 4xx.
- [ ] 5xx.
- [ ] Restart.
- [ ] Update.
- [ ] Migration.
- [ ] Duplicate operation.
- [ ] Conflict.

## Performance
- [ ] Startup.
- [ ] Memory.
- [ ] Large catalog.
- [ ] Large order.
- [ ] Large image.
- [ ] Queue performance.

### Gate P9
- [ ] Regression suite green.
- [ ] No known V1 blocker.

---

# P10 — Beta / Hardening

- [ ] Real WooCommerce stores tested.
- [ ] Multiple hosting/server conditions tested.
- [ ] Weak mobile network tested.
- [ ] Background restrictions tested.
- [ ] Large data tested.
- [ ] Crash monitoring reviewed.
- [ ] UX issues triaged.
- [ ] Critical/High issues resolved.

### Gate P10
- [ ] Beta acceptance signed off.

---

# P11 — Release

- [ ] Scope verified against `PRODUCT_VISION.md`.
- [ ] Scope verified against `ROADMAP.md`.
- [ ] Decisions verified against `DECISION_TRACKER.md`.
- [ ] Build Plan gates all passed.
- [ ] Release build verified.
- [ ] Signing verified.
- [ ] Migration verified.
- [ ] Final regression green.
- [ ] Known limitations documented.
- [ ] Release notes ready.
- [ ] GitHub release/tag ready.

## V1 Final Gate
- [ ] New Order Notification works.
- [ ] Orders management works.
- [ ] Products management works.
- [ ] Local-first works.
- [ ] Sync/retry/conflict works.
- [ ] Security requirements pass.
- [ ] No silent data loss known.
- [ ] No Critical/High blocker known.

---

# بررسی‌های اجباری مشترک در همه مراحل

- [ ] آیا این تغییر با Product Vision سازگار است؟
- [ ] آیا در Roadmap برای V1 مجاز است؟
- [ ] آیا Decision Tracker تصمیم مرتبط دارد؟
- [ ] آیا Architecture boundary شکسته شده؟
- [ ] آیا قابلیت آینده ناخواسته وارد V1 شده؟
- [ ] آیا Test قابل نوشتن است؟
- [ ] آیا Offline/Network failure رفتار مشخص دارد؟
- [ ] آیا امنیت Credential حفظ شده؟
- [ ] آیا Documentation لازم به‌روز شده؟

# قانون عبور

هیچ Phase بدون عبور از Gate خودش «تمام‌شده» محسوب نمی‌شود. اگر یک بررسی `[!]` باشد، مرحله Blocked است مگر اینکه Decision Tracker صراحتاً ریسک را پذیرفته باشد.
