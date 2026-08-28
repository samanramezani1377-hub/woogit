# WooGit Liquid Glass V1 — UI Reference

این پوشه کپی کامل فایل‌های پروژه مرجع `Liguid-glass` است و به عنوان مرجع رسمی طراحی رابط کاربری Liquid Glass پروژه WooGit نگهداری می‌شود.

## فایل‌ها

- `woogit-liquid-glass-v1-fixed.html` — نمونه کامل رابط کاربری و کد مرجع
- `index.html` — صفحه پیش‌نمایش مرجع
- `README.md` — مستندات مرجع
- `pages.yml` — Workflow مربوط به GitHub Pages در پروژه اصلی مرجع

## اصول طراحی

- طراحی موبایل‌محور و RTL
- کارت‌های با ارتفاع پویا بر اساس محتوا
- چیدمان عمودی مبتنی بر جریان عادی صفحه
- اسکرول عمودی Feed
- شفافیت چندلایه و `backdrop-filter: blur()`
- saturation، حاشیه نیمه‌شفاف و سایه‌های چندلایه
- هایلایت داخلی و sheen
- پس‌زمینه‌های رنگی شعاعی
- گوشه‌های گرد بزرگ
- انیمیشن‌های ظریف و پشتیبانی از `prefers-reduced-motion`

این مرجع یک Prototype پیشرفته Glassmorphism است و قرار نیست کد HTML آن مستقیماً جایگزین رابط Android شود؛ هدف آن حفظ مرجع بصری، ساختار، spacing، رفتار کارت‌ها و اصول Liquid Glass برای پیاده‌سازی Native در WooGit است.
