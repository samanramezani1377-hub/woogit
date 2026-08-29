# WooGit Product Import / Export

## Format

WooGit uses a versioned `.woogit` package for product transfer. The file is a ZIP container with:

- `manifest.json` — format/version/source metadata.
- `products.json` — product, category, attribute, image reference and variation data.
- `media/` — embedded product and variation image files.

Current format identifier is `woogit-products` and version `1`.

## Export

Export reads the product catalog available through the Product repository, includes variable-product variations, and attempts to embed every referenced image. Product image IDs and source URLs are retained in the package.

## Import

Import validates the package before processing. For the same store, product IDs are preferred and SKU is the fallback. For another store, SKU is used for matching so IDs from the source store are not treated as destination IDs.

Destination category IDs are resolved by category name when importing into another store. Images are uploaded through the existing MediaRepository boundary and the returned destination Media ID is used for product/variation association.

Product and variation mutations continue through the existing Create/Update repositories, so Local-first and Pending Queue behavior remain the source of truth for mutations.

## Compatibility

The package contains no credentials or store API keys. The source store URL is metadata only. Future format changes must increment the package version and preserve backward import compatibility where practical.
