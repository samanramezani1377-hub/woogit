package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipInputStream

private const val MEDIA_MAX_BYTES = 50L * 1024L * 1024L
private const val BUFFER = 8192

internal class ProductTransferMedia(
    private val d: V1PresentationDependencies,
    private val resolver: ContentResolver,
) {
    suspend fun upload(
        storeId: StoreId,
        source: Uri,
        products: List<TransferProduct>,
        onProgress: (ProductTransferProgress) -> Unit,
    ): TransferMediaOutcome = withContext(Dispatchers.IO) {
        // Media is deliberately isolated from the product import. Any failure here is
        // converted into a per-entry outcome; it must never abort the whole import.
        try {
            uploadInternal(storeId, source, products, onProgress)
        } catch (t: Throwable) {
            val message = "خواندن رسانه‌های انتقالی ناموفق بود: ${t.message ?: "خطای نامشخص"}"
            TechnicalErrorReporter.report(
                feature="Product Transfer",
                location="ProductTransferMedia.upload",
                operation="IMPORT_MEDIA",
                userMessage="انتقال تصاویر با خطای فنی مواجه شد.",
                throwable=t,
                type="PRODUCT_TRANSFER_MEDIA_EXCEPTION",
                details="storeId=${storeId.value}; source=$source",
            )
            TransferMediaOutcome(
                images = emptyMap(),
                failed = 1,
                errors = listOf(message),
                uploaded = 0,
                reused = 0,
            )
        }
    }

    private suspend fun uploadInternal(
        storeId: StoreId,
        source: Uri,
        products: List<TransferProduct>,
        onProgress: (ProductTransferProgress) -> Unit,
    ): TransferMediaOutcome {
        val destination = readDestinationMedia(storeId)
        val byUrl = destination.asSequence()
            .filter { it.src.isNotBlank() }
            .associateBy { canonicalUrl(it.src) }
        val byName = destination.asSequence()
            .filter { it.src.isNotBlank() }
            .groupBy { normalize(fileName(it.name ?: it.src)) }
        val sourceByFile = buildMap<String, TransferImage> {
            products.forEach { product ->
                product.images.forEach { put(it.file, it) }
                product.variations.forEach { variation -> variation.image?.let { put(it.file, it) } }
            }
        }
        val resolved = linkedMapOf<String, ProductImage>()
        val byContentHash = mutableMapOf<String, ProductImage>()
        val remoteHashCache = mutableMapOf<String, String?>()
        val sourceHashCache = mutableMapOf<String, String>()
        val errors = mutableListOf<String>()
        var failed = 0
        var uploaded = 0
        var reused = 0

        resolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory || !entry.name.startsWith("media/")) continue
                    val file = entry.name

                    // One malformed/unreadable media entry must not affect any other entry.
                    try {
                        val bytes = readLimited(zip)
                        val hash = sourceHashCache.getOrPut(file) { sha256(bytes) }
                        val sourceImage = sourceByFile[file]
                        val exact = sourceImage?.src?.let { candidateUrl ->
                            byUrl[canonicalUrl(candidateUrl)]?.takeIf {
                                remoteHash(it.src, remoteHashCache) == hash
                            }
                        }
                        val cached = byContentHash[hash]
                        val byFilename = if (exact == null && cached == null) {
                            val wantedName = normalize(fileName(sourceImage?.name ?: file))
                            byName[wantedName].orEmpty().asSequence()
                                .distinctBy { canonicalUrl(it.src) }
                                .mapNotNull { candidate ->
                                    remoteHash(candidate.src, remoteHashCache)
                                        ?.takeIf { it == hash }
                                        ?.let { candidate }
                                }
                                .firstOrNull()
                        } else null
                        val reusedImage = exact ?: cached ?: byFilename

                        if (reusedImage != null) {
                            resolved[file] = reusedImage
                            byContentHash.putIfAbsent(hash, reusedImage)
                            reused++
                            continue
                        }

                        when (val result = d.uploadMedia(storeId, fileName(file), bytes, mime(file))) {
                            is CoreResult.Success -> {
                                resolved[file] = result.value
                                byContentHash[hash] = result.value
                                uploaded++
                                onProgress(ProductTransferProgress("در حال آپلود تصاویر…", uploaded, -1))
                            }
                            is CoreResult.Failure -> {
                                failed++
                                val message = "آپلود رسانه $file ناموفق بود: ${result.error}"
                                errors += message
                                TechnicalErrorReporter.reportHandled(
                                    feature="Product Transfer",
                                    location="ProductTransferMedia.upload",
                                    operation="UPLOAD_MEDIA",
                                    userMessage="آپلود یک تصویر ناموفق بود.",
                                    technicalMessage=result.error.toString(),
                                    type="PRODUCT_TRANSFER_MEDIA_UPLOAD_ERROR",
                                    details="file=$file; storeId=${storeId.value}",
                                )
                            }
                        }
                    } catch (t: Throwable) {
                        failed++
                        val message = "پردازش رسانه $file ناموفق بود: ${t.message ?: "خطای نامشخص"}"
                        errors += message
                        TechnicalErrorReporter.report(
                            feature="Product Transfer",
                            location="ProductTransferMedia.entry",
                            operation="PROCESS_MEDIA_ENTRY",
                            userMessage="پردازش یک تصویر انتقالی ناموفق بود.",
                            throwable=t,
                            type="PRODUCT_TRANSFER_MEDIA_ENTRY_ERROR",
                            details="file=$file; storeId=${storeId.value}; source=$source",
                        )
                    }
                }
            }
        } ?: run {
            return TransferMediaOutcome(
                images = emptyMap(),
                failed = 1,
                errors = listOf("فایل رسانه قابل خواندن نیست."),
                uploaded = 0,
                reused = 0,
            )
        }
        return TransferMediaOutcome(resolved, failed, errors, uploaded, reused)
    }

    private suspend fun readDestinationMedia(storeId: StoreId): List<ProductImage> =
        ProductTransferRepositoryReader(d).media(storeId)

    private fun readLimited(zip: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER)
        var total = 0L
        while (true) {
            val n = zip.read(buffer)
            if (n <= 0) break
            total += n
            require(total <= MEDIA_MAX_BYTES) { "یکی از تصاویر بیش از حد مجاز است." }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun remoteHash(src: String, cache: MutableMap<String, String?>): String? {
        val key = canonicalUrl(src)
        if (key.isBlank()) return null
        if (cache.containsKey(key)) return cache[key]
        val value = remoteBytes(key)?.let(::sha256)
        cache[key] = value
        return value
    }

    private fun remoteBytes(src: String): ByteArray? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(src).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "WooGit-Media-Dedup/1.0")
            val length = connection.contentLengthLong
            if (length > MEDIA_MAX_BYTES) return null
            connection.inputStream.use { input ->
                val initialCapacity = if (length in 1..Int.MAX_VALUE.toLong()) length.toInt() else BUFFER
                val out = ByteArrayOutputStream(initialCapacity)
                val buffer = ByteArray(BUFFER)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    total += n
                    if (total > MEDIA_MAX_BYTES) return null
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        } catch (_: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun canonicalUrl(value: String): String = value.trim().removeSuffix("/")
    private fun fileName(value: String): String = value.substringBefore('?').substringAfterLast('/').trim()
    private fun mime(value: String): String = when (value.substringBefore('?').substringAfterLast('.').lowercase(Locale.ROOT)) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        else -> "image/jpeg"
    }
}