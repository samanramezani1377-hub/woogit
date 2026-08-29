package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.entity.StoreId
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
        val destination = readDestinationMedia(storeId)

        // Cheap metadata indexes only. Destination bytes are fetched lazily and cached.
        val byUrl = destination.asSequence()
            .filter { it.src.isNotBlank() }
            .associateBy { canonicalUrl(it.src) }
        val byName = destination.asSequence()
            .filter { it.src.isNotBlank() }
            .groupBy { normalize(fileName(it.name ?: it.src)) }

        val sourceByFile = buildMap<String, TransferImage> {
            products.forEach { product ->
                product.images.forEach { put(it.file, it) }
                product.variations.forEach { variation ->
                    variation.image?.let { put(it.file, it) }
                }
            }
        }

        val resolved = linkedMapOf<String, ProductImage>()
        // Source content hash -> destination media. This also deduplicates repeated package files.
        val byContentHash = mutableMapOf<String, ProductImage>()
        // Canonical destination URL -> hash. Both successful and failed lookups are cached.
        val remoteHashCache = mutableMapOf<String, String?>()
        // Package file -> hash, so repeated references to the same file never hash the bytes twice.
        val sourceHashCache = mutableMapOf<String, String>()
        val errors = mutableListOf<String>()
        var failed = 0
        var uploaded = 0

        resolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory || !entry.name.startsWith("media/")) continue

                    val file = entry.name
                    val bytes = readLimited(zip)
                    val hash = sourceHashCache.getOrPut(file) { sha256(bytes) }
                    val sourceImage = sourceByFile[file]

                    val exact = sourceImage?.src?.let { candidateUrl ->
                        byUrl[canonicalUrl(candidateUrl)]?.takeIf {
                            remoteHash(it.src, remoteHashCache) == hash
                        }
                    }

                    val cached = byContentHash[hash]

                    // Filename matching is the expensive fallback. Each destination URL is
                    // considered at most once per import because remoteHash has a canonical URL cache.
                    val byFilename = if (exact == null && cached == null) {
                        val wantedName = normalize(fileName(sourceImage?.name ?: file))
                        byName[wantedName].orEmpty()
                            .asSequence()
                            .distinctBy { canonicalUrl(it.src) }
                            .mapNotNull { candidate ->
                                remoteHash(candidate.src, remoteHashCache)
                                    ?.takeIf { it == hash }
                                    ?.let { candidate }
                            }
                            .firstOrNull()
                    } else null

                    val reused = exact ?: cached ?: byFilename
                    if (reused != null) {
                        resolved[file] = reused
                        byContentHash.putIfAbsent(hash, reused)
                    } else {
                        when (val result = d.uploadMedia(
                            storeId,
                            fileName(file),
                            bytes,
                            mime(file),
                        )) {
                            is CoreResult.Success -> {
                                resolved[file] = result.value
                                byContentHash[hash] = result.value
                                uploaded++
                                onProgress(ProductTransferProgress("در حال آپلود تصاویر…", uploaded, -1))
                            }
                            is CoreResult.Failure -> {
                                failed++
                                errors += "آپلود رسانه $file ناموفق بود: ${result.error}"
                            }
                        }
                    }
                }
            }
        } ?: error("فایل قابل خواندن نیست.")

        TransferMediaOutcome(resolved, failed, errors, uploaded)
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

    private fun fileName(value: String): String =
        value.substringBefore('?').substringAfterLast('/').trim()

    private fun mime(value: String): String =
        when (value.substringBefore('?').substringAfterLast('.').lowercase(Locale.ROOT)) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            else -> "image/jpeg"
        }
}