# Commerce Navigation

Commerce capabilities are no longer modeled as seven screens inside `CommerceCenterScreen`.

## E11 destinations

- `E11Routes.COMMERCE_BARCODE` → Barcode / SKU
- `E11Routes.COMMERCE_BULK_ORDERS` → Bulk order status operations
- `E11Routes.COMMERCE_INVENTORY` → Inventory filtering
- `E11Routes.COMMERCE_ANALYTICS` → Advanced sales analytics
- `E11Routes.COMMERCE_INVOICE` → Invoice workflow
- `E11Routes.COMMERCE` → Commerce center for Customers and Coupons

Each capability route uses `CommerceFeatureRouteScreen`, so the feature is an actual E11 navigation destination rather than local `selected` state inside the Commerce center.

## Dashboard

The Dashboard `بارکد و SKU` quick action navigates directly to `E11Routes.COMMERCE_BARCODE`; it does not navigate to the Commerce center first.

## Placement intent

- Barcode / SKU: direct Dashboard entry + independent route.
- Bulk Orders: independent route owned by E11 navigation.
- Inventory: independent route owned by E11 navigation.
- Analytics: independent route owned by E11 navigation.
- Invoice: independent route owned by E11 navigation.
- Customers: Commerce center.
- Coupons: Commerce center.
