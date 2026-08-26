# WooGit — Security & Permissions

## هدف

WooGit باید از همان ابتدا برای چندکاربره شدن و کنترل دسترسی دقیق آماده باشد، حتی اگر نسخه اول عملاً یک کاربر با دسترسی کامل داشته باشد.

## مدل دسترسی

Core باید مفهوم مستقل **User، Role و Permission** داشته باشد.

نقش‌های اولیه پیشنهادی:

- **Owner/Admin:** دسترسی کامل
- **Order Manager:** مدیریت سفارش‌ها و اطلاعات مرتبط
- **Product Manager:** مدیریت محصولات
- **Viewer:** فقط مشاهده
- **Custom Role:** نقش سفارشی با مجموعه Permissionهای انتخاب‌شده

این نقش‌ها در نسخه اول الزاماً UI کامل ندارند، اما مدل Core نباید به یک نقش ثابت محدود شود.

## Permissionهای قابل توسعه

Permissionها باید granular و قابل توسعه باشند. نمونه:

- `orders.read`
- `orders.edit`
- `orders.status.update`
- `orders.notes.manage`
- `products.read`
- `products.create`
- `products.edit`
- `products.delete`
- `products.inventory.manage`
- `notifications.read`
- `notifications.manage`
- `users.read`
- `users.manage`
- `settings.manage`

Permissionهای جدید نباید نیازمند بازطراحی Role Model باشند.

## Multi-Store

هر User/Role/Permission باید در آینده بتواند در محدوده‌ی یک یا چند Store اعمال شود. دسترسی کاربر به فروشگاه A نباید به‌طور خودکار به فروشگاه B تعمیم پیدا کند.

## امنیت

Credentialهای WooCommerce و اطلاعات حساس Store Connection باید از UI و لاگ‌ها جدا باشند و با Storage امن دستگاه نگهداری شوند. Permission check باید در Core وجود داشته باشد و فقط به UI وابسته نباشد.

### رفتار UI هنگام نبود Permission

وقتی کاربر به یک عملیات/کنترل خاص دسترسی ندارد، آن کنترل به‌سادگی **پنهان** می‌شود؛ پیام خطای جداگانه‌ای نمایش داده نمی‌شود.

## اصل آینده‌نگری

نسخه اول می‌تواند Single-User/Full-Access باشد، اما Core نباید با فرض «همیشه فقط یک مدیر با دسترسی کامل وجود دارد» طراحی شود.
