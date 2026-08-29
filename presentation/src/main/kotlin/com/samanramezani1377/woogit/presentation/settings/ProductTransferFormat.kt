package com.samanramezani1377.woogit.presentation.settings

/**
 * Version contract for the .woogit product-transfer package.
 *
 * IMPORTANT:
 * - FORMAT_VERSION versions the logical product-transfer schema.
 * - LAYOUT_VERSION versions the physical ZIP layout (entry names/locations).
 * - Both are temporary v1 contracts until explicitly promoted to FINAL.
 * - Do not change the v1 layout in-place. A structural change requires a new
 *   layout version and a reader for the older version.
 */
internal object ProductTransferFormat {
    const val FORMAT = "woogit-products"

    // Temporary v1. Do NOT mark this FINAL until explicitly requested.
    const val FORMAT_VERSION = 1
    const val LAYOUT_VERSION = 1

    const val MANIFEST_ENTRY = "manifest.json"
    const val PRODUCTS_ENTRY = "products.json"
    const val MEDIA_DIRECTORY = "media/"

    val SUPPORTED_FORMAT_VERSIONS = 1..FORMAT_VERSION
    val SUPPORTED_LAYOUT_VERSIONS = 1..LAYOUT_VERSION
}

/**
 * WooGit .woogit layout v1 (TEMPORARY).
 *
 * ZIP root:
 *   manifest.json
 *   products.json
 *   media/
 *     p-<product-id>-<index>.<ext>
 *     v-<variation-id>.<ext>
 *
 * manifest.json is the package contract header and declares both version
 * dimensions. products.json contains the serialized ProductTransferPackage.
 * Media files are payload entries referenced by TransferImage.file.
 *
 * FINALIZATION RULE:
 * When v1 is explicitly declared FINAL, this document becomes immutable.
 * No entry may be renamed, moved, removed, or structurally repurposed in v1.
 * Any future structural change must use a new LAYOUT_VERSION and retain a
 * compatible reader for v1 packages.
 */
internal object ProductTransferLayoutV1Temporary
