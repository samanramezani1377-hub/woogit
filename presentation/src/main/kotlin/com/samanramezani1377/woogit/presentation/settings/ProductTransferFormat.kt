package com.samanramezani1377.woogit.presentation.settings

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Versioned .woogit format registry.
 *
 * Version identifiers are metadata, not secrets or encryption keys.
 * Export selects the active layout contract; Import selects the matching
 * contract from the same registry using manifest metadata.
 *
 * V1 is intentionally TEMPORARY for now. It becomes immutable only when the
 * product owner explicitly declares V1 FINAL. Until then its layout may change.
 */
internal object ProductTransferFormat {
    const val FORMAT = "woogit-products"
    const val FORMAT_VERSION = 1
    const val LAYOUT_VERSION = 1

    const val MANIFEST_ENTRY = "manifest.json"
    const val PRODUCTS_ENTRY = "products.json"
    const val MEDIA_DIRECTORY = "media/"

    private const val PRODUCT_MEDIA_PREFIX = "media/p-"
    private const val VARIATION_MEDIA_PREFIX = "media/v-"

    /** The one layout currently used by Export. */
    val ACTIVE_LAYOUT: Layout = LayoutV1

    /** Every layout that Import can read. Keep old entries when adding V2+. */
    private val READABLE_LAYOUTS: Map<Int, Layout> = mapOf(
        LayoutV1.version to LayoutV1,
    )

    val SUPPORTED_FORMAT_VERSIONS: IntRange = 1..FORMAT_VERSION
    val SUPPORTED_LAYOUT_VERSIONS: Set<Int> = READABLE_LAYOUTS.keys

    fun layoutForImport(layoutVersion: Int): Layout =
        READABLE_LAYOUTS[layoutVersion]
            ?: error("نسخه چیدمان فایل WooGit پشتیبانی نمی‌شود: $layoutVersion")

    fun layoutForExport(): Layout = ACTIVE_LAYOUT

    fun productImagePath(productId: String, imageIndex: Int, extension: String): String =
        ACTIVE_LAYOUT.productImagePath(productId, imageIndex, extension)

    fun variationImagePath(variationId: String, extension: String): String =
        ACTIVE_LAYOUT.variationImagePath(variationId, extension)

    /**
     * Converts legacy V1 media paths emitted by the transfer service into the
     * physical path defined by the active layout. This keeps the service's
     * logical media intent separate from the physical ZIP layout.
     */
    fun canonicalizeExportPath(path: String): String = when {
        path == MANIFEST_ENTRY || path == PRODUCTS_ENTRY -> path
        path.startsWith(PRODUCT_MEDIA_PREFIX) -> path
        path.startsWith(VARIATION_MEDIA_PREFIX) -> path
        else -> path
    }

    /** The only low-level ZIP entry writer used by product transfer. */
    fun writeEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        require(ACTIVE_LAYOUT.acceptsEntryPath(path)) {
            "مسیر فایل خارج از هسته Layout v${ACTIVE_LAYOUT.version} است: $path"
        }
        zip.putNextEntry(ZipEntry(path))
        zip.write(bytes)
        zip.closeEntry()
    }

    fun isAllowedEntryPath(path: String): Boolean = ACTIVE_LAYOUT.acceptsEntryPath(path)

    interface Layout {
        val version: Int
        fun productImagePath(productId: String, imageIndex: Int, extension: String): String
        fun variationImagePath(variationId: String, extension: String): String
        fun acceptsEntryPath(path: String): Boolean
    }

    /** WooGit Layout V1 — TEMPORARY until explicitly finalized. */
    private object LayoutV1 : Layout {
        override val version: Int = 1

        override fun productImagePath(productId: String, imageIndex: Int, extension: String): String =
            "$PRODUCT_MEDIA_PREFIX$productId-$imageIndex.$extension"

        override fun variationImagePath(variationId: String, extension: String): String =
            "$VARIATION_MEDIA_PREFIX$variationId.$extension"

        override fun acceptsEntryPath(path: String): Boolean = when {
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
}
