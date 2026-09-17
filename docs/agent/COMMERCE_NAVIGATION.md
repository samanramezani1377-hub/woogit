# Commerce Navigation

Commerce is a small folder-like section for **Customers** and **Coupons** only. It is not a parent, unified center, data center, or owner of the other commerce-related tools.

## E11 destinations

- `E11Routes.COMMERCE` → Commerce folder for Customers and Coupons only.
- `E11Routes.DASHBOARD_BARCODE` → Barcode / SKU
- `E11Routes.ORDERS_BULK` → Bulk order status operations
- `E11Routes.PRODUCTS_INVENTORY` → Inventory filtering
- `E11Routes.DASHBOARD_ANALYTICS` → Advanced sales analytics
- `E11Routes.ORDER_INVOICE` → Invoice workflow

The five standalone capabilities are reached directly from the section that owns them. They do not pass through the Commerce center and must not use Commerce as their state owner.

## Commerce center

`CommerceCenterScreen` contains only:

- Customers
- Coupons

Customers and Coupons open inside the Commerce folder using their dedicated feature state/view models. The center does not load or own Barcode, Bulk Orders, Inventory, Analytics, or Invoice.

Commerce is intentionally only a navigation/folder convenience so Dashboard does not become cluttered. It has no additional architectural role.

## Feature state ownership

Feature presentation state is owned by the feature itself:

- Barcode → `BarcodeFeatureViewModel`
- Inventory → `InventoryFeatureViewModel`
- Bulk Orders → `BulkOrdersFeatureViewModel`
- Invoice → `InvoiceFeatureViewModel`
- Customers → `CustomerCommerceViewModel` / `CustomerRuntime`
- Coupons → `CouponCommerceViewModel` and its existing runtime/data path
- Analytics → existing analytics route/runtime and local analytics repository

`CommerceViewModel` is not a shared owner for these unrelated features and must not be reintroduced for that purpose.

## Data ownership

Feature separation does **not** require a new central data layer. All features continue to use the existing repositories, local database/cache, sync engine, and runtime contracts.

Cross-domain reads are allowed when the feature genuinely needs them, but they must use the existing data/runtime infrastructure rather than `CommerceViewModel` or `CommerceCenterScreen`.

Examples:

- Customers may use order data for customer order summaries.
- Coupons may use product/customer data for coupon restrictions and selectors.
- Orders may use customer/coupon data for aggregation and coupon analytics.
- Analytics may use orders and products through the existing local analytics repository.

## Placement intent

- Barcode / SKU: Dashboard entry + independent route.
- Analytics: Dashboard entry + independent route.
- Bulk Orders: Orders entry + independent route.
- Inventory: Products entry + independent route.
- Invoice: Order Detail entry + independent route.
- Customers: Commerce folder.
- Coupons: Commerce folder.

## UI preservation

The standalone feature screens keep their existing presentation and behavior. The architecture change separates state ownership without introducing a new visual system or replacing the existing Liquid Glass page shells.

Customers and Coupons UI should remain visually and behaviorally unchanged while their state is owned by their own feature ViewModels. A temporary `CommerceUiState` compatibility adapter is acceptable only where an existing page contract still requires it; it is an adapter, not authoritative feature state.

## Prohibited architecture

Do not reintroduce:

- a unified Commerce hub containing all seven capabilities;
- Commerce as a data center;
- a shared Commerce ViewModel for unrelated features;
- Commerce-owned refresh loops for unrelated pages;
- routing unrelated features through Commerce solely to obtain state or data.

## Coupon management

Coupon management is intentionally individual-only. The coupon page does not expose multi-selection, bulk editing, bulk save, or bulk-edit controls.

Selecting a coupon opens an individual edit dialog for its discount amount. The existing Commerce coupon batch endpoint is reused with a single coupon ID, so this does not introduce a new API contract or a bulk-edit UX.

Coupon cards expose the fields already supplied by `WooCouponCommerceDto`: code, amount, discount type, expiry, total usage, usage limit, and per-user usage limit.
