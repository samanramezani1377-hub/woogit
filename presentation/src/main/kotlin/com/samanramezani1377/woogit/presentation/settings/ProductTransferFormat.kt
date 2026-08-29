package com.samanramezani1377.woogit.presentation.settings

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Single source of truth for the .woogit package contract.
 *
 * TEMPORARY v1: this contract is intentionally changeable until the product
 * transfer implementation is explicitly frozen. Once v1 is declared FINAL,
 * the physical layout below becomes immutable and any structural change must
 * introduce a new layout version and a compatible reader for v1.
 */
internal object ProductTransferFormat {
    const val FORMAT = "woogit-products"

    // TEMPORARY v1. Do not treat this as immutable yet.
    const val FORMAT_VERSION = 1
    const val LAYOUT_VERSION = 1

    const val MANIFEST_ENTRY = "manifest.json"
    const val PRODUCTS_ENTRY = "products.json"
    const val MEDIA_DIRECTORY = "media/"

    private const val PRODUCT_MEDIA_PREFIX = "media/p-"
    private const val VARIATION_MEDIA_PREFIX = "media/v-"

    val SUPPORTED_FORMAT_VERSIONS = 1..FORMAT_VERSION
    val SUPPORTED_LAYOUT_VERSIONS = 1..LAYOUT_VERSION

    fun productImagePath(productId: String, imageIndex: Int, extension: String): String =
        "$PRODUCT_MEDIA_PREFIX$productId-$imageIndex.$extension"

    fun variationImagePath(variationId: String, extension: String): String =
        "$VARIATION_MEDIA_PREFIX$variationId.$extension"

    /**
     * The only low-level ZIP entry writer used by product transfer.
     * Keeping the path validation here prevents Export code from silently
     * creating a package that does not conform to the active layout.
     */
    fun writeEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        require(isAllowedEntryPath(path)) { "مسیر فایل خارج از هسته Layout v1 است: $path" }
        zip.putNextEntry(ZipEntry(path))
        zip.write(bytes)
        zip.closeEntry()
    }

    fun isAllowedEntryPath(path: String): Boolean = when {
        path == MANIFEST_ENTRY || path == PRODUCTS_ENTRY -> true
        path.startsWith(PRODUCT_MEDIA_PREFIX) -> isMediaFileName(path)
        path.startsWith(VARIATION_MEDIA_PREFIX) -> isMediaFileName(path)
        else -> false
    }

    private fun isMediaFileName(path: String): Boolean {
        if (path.contains("..") || path.startsWith("/") || path.contains('\\')) return false
        val fileName = path.substringAfterLast('/')
        if (fileName.isBlank() || fileName.contains('/')) return false
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in setOf("jpg", "jpeg", "png", "webp", "gif", "svg")
    }
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
 * FINALIZATION RULE:
 * When v1 is explicitly declared FINAL, this document becomes immutable.
 * No entry may be renamed, moved, removed, or structurally repurposed in v1.
 * Any future structural change must use a new LAYOUT_VERSION and retain a
 * compatible reader for v1 packages.
 */