package com.samanramezani1377.woogit.presentation.settings

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Temporary v1 .woogit package layout kernel.
 *
 * IMPORTANT: this is intentionally TEMPORARY and may change while the
 * Import/Export implementation is being completed. It must not be marked
 * FINAL/IMMUTABLE until the product-transfer format is explicitly frozen.
 *
 * Once v1 is declared final, this file becomes the immutable contract for
 * the physical ZIP layout. Any future physical-layout change must use v2.
 */
internal object ProductTransferLayoutV1Temporary {
    const val FORMAT = "woogit-products"
    const val VERSION = 1
    const val LAYOUT_VERSION = 1

    const val MANIFEST_PATH = "manifest.json"
    const val PRODUCTS_PATH = "products.json"
    const val MEDIA_DIRECTORY = "media/"

    private const val PRODUCT_MEDIA_PREFIX = "media/p-"
    private const val VARIATION_MEDIA_PREFIX = "media/v-"

    fun productImagePath(productId: String, imageIndex: Int, extension: String): String =
        "$PRODUCT_MEDIA_PREFIX$productId-$imageIndex.$extension"

    fun variationImagePath(variationId: String, extension: String): String =
        "$VARIATION_MEDIA_PREFIX$variationId.$extension"

    fun writeEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        require(isAllowedEntryPath(path)) { "مسیر فایل خارج از هسته Layout v1 است: $path" }
        zip.putNextEntry(ZipEntry(path))
        zip.write(bytes)
        zip.closeEntry()
    }

    fun isAllowedEntryPath(path: String): Boolean = when {
        path == MANIFEST_PATH || path == PRODUCTS_PATH -> true
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
