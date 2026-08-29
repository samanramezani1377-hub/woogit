# WooGit Product Import / Export

## Format contract

WooGit uses a versioned `.woogit` package for product transfer. The file is a ZIP container with:

- `manifest.json` — format, logical schema version, physical layout version and source metadata.
- `products.json` — product, category, attribute, image reference and variation data.
- `media/` — embedded product and variation image files.

The active temporary contract is:

- Format: `woogit-products`
- Format version: `1` (TEMPORARY)
- Layout version: `1` (TEMPORARY)

`ProductTransferFormat.kt` is the single source of truth for the package identifier, supported versions and physical ZIP entry layout. ZIP writes are routed through that kernel, so Export cannot silently create an entry outside the active layout contract. Import validates the same contract before mutation.

### Temporary v1 physical layout

```text
.woogit (ZIP)
├── manifest.json
├── products.json
└── media/
    ├── p-<product-id>-<index>.<ext>
    └── v-<variation-id>.<ext>
```

This v1 layout is intentionally **not final yet** and may change while Import/Export is being completed.

## Versioning rules

`version` identifies the logical transfer schema. `layoutVersion` identifies the physical ZIP layout. They are independent so a future schema-only change does not unnecessarily change file placement, and a physical-layout change can be versioned independently.

When v1 is explicitly declared FINAL, its physical layout becomes immutable. No file may be renamed, moved, removed, or structurally repurposed inside layout v1. Any later physical-layout change must introduce a new layout version and retain a compatible reader for v1 packages.

Until that explicit finalization, v1 remains a temporary contract and may be revised in-place.

## Export

Export reads the product catalog available through the Product repository, includes variable-product variations, and embeds referenced images. Every ZIP entry is written through the active layout kernel. Product image IDs and source URLs are retained in the package.

## Import

Import validates the package before mutation. It verifies the format, logical version and layout version against the supported contract, then reads only entries permitted by that layout. For the same store, product IDs are preferred and SKU is the fallback. For another store, SKU is used for matching so IDs from the source store are not treated as destination IDs.

Destination category IDs are resolved through the destination category mapping. Images are uploaded through the existing MediaRepository boundary and the returned destination Media ID is used for product/variation association.

Product and variation mutations continue through the existing Create/Update repositories, so Local-first and Pending Queue behavior remain the source of truth for mutations.

## Compatibility

The package contains no credentials or store API keys. The source store URL is metadata only. Older packages remain importable when their format/layout versions are supported. Future incompatible physical layouts must use a new layout version rather than silently changing v1.
