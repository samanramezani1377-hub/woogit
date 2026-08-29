package com.samanramezani1377.woogit.presentation.settings

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Versioned .woogit format registry. V1 is TEMPORARY until explicitly finalized. */
internal object ProductTransferFormat {
    const val FORMAT = "woogit-products"
    const val FORMAT_VERSION = 1
    const val LAYOUT_VERSION = 1
    const val MANIFEST_ENTRY = "manifest.json"
    const val PRODUCTS_ENTRY = "products.json"
    const val MEDIA_DIRECTORY = "media/"

    private const val PRODUCT_MEDIA_PREFIX = "media/p-"
    private const val VARIATION_MEDIA_PREFIX = "media/v-"

    val ACTIVE_LAYOUT: Layout = LayoutV1
    private val READABLE_LAYOUTS: Map<Int, Layout> = mapOf(LayoutV1.version to LayoutV1)
    val SUPPORTED_FORMAT_VERSIONS: IntRange = 1..FORMAT_VERSION
    val SUPPORTED_LAYOUT_VERSIONS: Set<Int> = READABLE_LAYOUTS.keys

    fun layoutForImport(layoutVersion: Int): Layout = READABLE_LAYOUTS[layoutVersion]
        ?: error("نسخه چیدمان فایل WooGit پشتیبانی نمی‌شود: $layoutVersion")

    fun layoutForExport(): Layout = ACTIVE_LAYOUT

    fun productImagePath(productId: String, imageIndex: Int, extension: String): String =
        ACTIVE_LAYOUT.productImagePath(productId, imageIndex, extension)

    fun variationImagePath(variationId: String, extension: String): String =
        ACTIVE_LAYOUT.variationImagePath(variationId, extension)

    /** Every ZIP path written by Export is canonicalized by the active layout. */
    fun canonicalizeExportPath(path: String): String = when {
        path == MANIFEST_ENTRY || path == PRODUCTS_ENTRY -> path
        path.startsWith(PRODUCT_MEDIA_PREFIX) -> {
            val file = path.removePrefix(PRODUCT_MEDIA_PREFIX)
            val dash = file.lastIndexOf('-')
            val dot = file.lastIndexOf('.')
            if (dash > 0 && dot > dash) {
                val productId = file.substring(0, dash)
                val index = file.substring(dash + 1, dot).toIntOrNull()
                if (index != null) productImagePath(productId, index, file.substring(dot + 1)) else path
            } else path
        }
        path.startsWith(VARIATION_MEDIA_PREFIX) -> {
            val file = path.removePrefix(VARIATION_MEDIA_PREFIX)
            val dot = file.lastIndexOf('.')
            if (dot > 0) variationImagePath(file.substring(0, dot), file.substring(dot + 1)) else path
        }
        else -> path
    }

    /** Single ZIP write gateway for the transfer feature. */
    fun writeEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        val canonicalPath = canonicalizeExportPath(path)
        require(ACTIVE_LAYOUT.acceptsEntryPath(canonicalPath)) {
            "مسیر فایل خارج از هسته Layout v${ACTIVE_LAYOUT.version} است: $canonicalPath"
        }
        zip.putNextEntry(ZipEntry(canonicalPath))
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

    /** WooGit Layout V1 — TEMPORARY. */
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
            return fileName.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "svg")
        }
    }
}
