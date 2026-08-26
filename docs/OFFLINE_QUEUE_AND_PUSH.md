# WooGit — Offline Queue & Push

## تصمیم اصلی

وقتی کاربر یک تغییر را در WooGit انجام می‌دهد، تغییر باید **بلافاصله به‌صورت Local ثبت شود** و Commit/Version محلی Entity همان لحظه افزایش یابد؛ کاربر نباید برای دیدن نتیجه منتظر سرور بماند.

سپس Sync Engine نتیجه‌ی ثبت روی سرور را بررسی می‌کند.

## جریان تغییر

```text
User Edit
   ↓
Local Transaction
   ↓
Entity Commit + 1
   ↓
UI immediately reflects change
   ↓
Pending Change Queue
   ↓
Try Push to WooCommerce
   ├── Success → mark Synced
   └── Failure / Offline → keep Pending
                         ↓
                    retry later
```

## Pending Queue

هر تغییر موفق‌نشده باید به‌صورت durable در صف محلی باقی بماند تا با قطع اینترنت، بسته شدن برنامه یا restart دستگاه از بین نرود.

هر Pending Change باید حداقل اطلاعات لازم برای Push مجدد را داشته باشد، از جمله:

- Entity type
- Entity ID
- Operation type
- Local version/commit
- Payload یا Patch لازم
- زمان ایجاد
- تعداد Retry
- وضعیت Sync
- آخرین خطا در صورت وجود

## Retry

وقتی اتصال دوباره برقرار شد، Sync Engine باید تغییرات Pending را به‌ترتیب مناسب Push کند.

Retry نباید باعث ایجاد تغییر تکراری روی سرور شود؛ عملیات Push باید تا حد امکان idempotent باشد.

## Server Confirmation

افزایش Commit محلی به معنی موفقیت سرور نیست.

دو وضعیت باید از هم جدا باشند:

- **Local Commit:** تغییر موردنظر کاربر ثبت و قابل مشاهده شده است.
- **Server Sync:** همان تغییر با موفقیت روی WooCommerce ثبت و تأیید شده است.

در نسخه اول UI فقط سه وضعیت ساده را به کاربر نمایش می‌دهد: `Pending` (معلق)، `Synced` (همگام‌شده) و `Failed` (ناموفق)، بدون تاریخچه کامل تغییرات؛ این نمایش نباید سرعت مسیر اصلی کاربر را کاهش دهد.

علاوه بر این، یک نشانگر کوچک و همیشه‌قابل‌مشاهده‌ی وضعیت اتصال/سنک (Status Dot) بالای صفحه قرار دارد: سبز = متصل و همگام، زرد = اتصال ضعیف یا در حال تلاش مجدد، قرمز = قطع یا ناموفق. خطاهای شبکه موقت به‌صورت خاموش در پس‌زمینه Retry می‌شوند و کاربر با پیام مزاحم مواجه نمی‌شود؛ فقط خطاهای مهم مانند خطای احراز هویت با پیام کوتاه + دکمه تلاش مجدد نمایش داده می‌شوند.

## Conflict

اگر در فاصله‌ی Pending بودن تغییر، همان Entity روی سرور نیز تغییر کرده باشد، Sync Engine نباید کورکورانه overwrite کند. باید نسخه‌ها را مقایسه و Conflict را برای استراتژی حل تعارض آماده کند.

## Performance

- Local transaction باید کوتاه و سریع باشد.
- Push نباید UI را block کند.
- Retry باید پس‌زمینه‌ای باشد.
- تغییرات قابل batch شدن باید در یک Push مناسب گروه‌بندی شوند.
- هنگام Launch، بررسی Pending Queue نباید باعث قفل شدن UI شود.
- WorkManager/مکانیزم معادل باید برای ادامه‌ی Sync در پس‌زمینه استفاده شود.
