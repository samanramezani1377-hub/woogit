# Barcode / SKU Scanner

## Purpose

The Barcode / SKU Scanner is a fast product-lookup workflow for store staff. Its job is to turn a physical barcode scan or manually entered SKU into the matching WooCommerce product and then open that product in the normal WooGit product detail flow.

The camera is only an input method. The capability is product resolution, not order management.

## User flow

`Dashboard -> Barcode / SKU Scanner -> Camera or manual SKU -> BarcodeResolver -> matching Product -> Product Detail`

A scan must never crash the app. An unknown value produces an explicit empty/not-found state and leaves the scanner ready for another attempt.

## Current resolution contract

The current domain model exposes `Product.sku` but does not expose a first-class barcode/GTIN field. Therefore the current resolver can reliably resolve values that are represented by WooCommerce SKU. The camera may physically scan a barcode whose value is used as the product SKU.

The scanner must not present order-number lookup as part of the product-scanner UX. Order lookup belongs to order workflows.

If first-class product barcodes are introduced later, the correct extension point is the domain/data mapping layer: expose the store's barcode/GTIN field and make `BarcodeResolver` match it alongside SKU. The UI should not guess WooCommerce meta keys.

## UI contract

The scanner route uses the same WooGit Glass presentation system as the rest of the app:

- `GlassScaffold`
- `GlassTopBar`
- `GlassCard`
- `GlassSearchField`
- `GlassPrimaryAction`
- shared loading/empty/error states

The scanner is a dedicated E11 destination. It must not be rendered as a card or nested pseudo-page inside Commerce Center.

## Implementation

- Presentation screen: `presentation/.../commerce/BarcodeScannerScreen.kt`
- Feature route: `presentation/.../commerce/CommerceFeatureRouteScreen.kt`
- Resolver: `core/.../commerce/BarcodeResolver.kt`
- Android camera activity: `app/.../commerce/CommerceScannerActivity.kt`
- Camera launch control: `presentation/.../commerce/CommerceCameraScanButton.kt`

The ViewModel continues to own resolution and store data loading; the screen only owns transient input text and navigation callbacks.
