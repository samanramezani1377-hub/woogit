# Commerce Navigation

Commerce is a small folder-like section, not a parent or hub for the other commerce-related tools.

## E11 destinations

- `E11Routes.COMMERCE` → Commerce folder for Customers and Coupons only.
- `E11Routes.DASHBOARD_BARCODE` → Barcode / SKU
- `E11Routes.ORDERS_BULK` → Bulk order status operations
- `E11Routes.PRODUCTS_INVENTORY` → Inventory filtering
- `E11Routes.DASHBOARD_ANALYTICS` → Advanced sales analytics
- `E11Routes.ORDER_INVOICE` → Invoice workflow

The five standalone capabilities are reached directly from the section that owns them. They do not pass through the Commerce center.

## Commerce center

`CommerceCenterScreen` contains only:

- Customers
- Coupons

Customers and Coupons open inside the Commerce folder using their dedicated feature state/view models. The center does not load or own Barcode, Bulk Orders, Inventory, Analytics, or Invoice.

## Placement intent

- Barcode / SKU: Dashboard entry + independent route.
- Analytics: Dashboard entry + independent route.
- Bulk Orders: Orders entry + independent route.
- Inventory: Products entry + independent route.
- Invoice: Order Detail entry + independent route.
- Customers: Commerce folder.
- Coupons: Commerce folder.

## UI preservation

The standalone feature screens keep their existing presentation and behavior. This change only removes the Commerce ownership/routing relationship and gives each capability a route under its actual section.

## Coupon management

Coupon management is intentionally individual-only. The coupon page does not expose multi-selection, bulk editing, bulk save, or bulk-edit controls.

Selecting a coupon opens an individual edit dialog for its discount amount. The existing Commerce coupon batch endpoint is reused with a single coupon ID, so this does not introduce a new API contract or a bulk-edit UX.

Coupon cards expose the fields already supplied by `WooCouponCommerceDto`: code, amount, discount type, expiry, total usage, usage limit, and per-user usage limit.
