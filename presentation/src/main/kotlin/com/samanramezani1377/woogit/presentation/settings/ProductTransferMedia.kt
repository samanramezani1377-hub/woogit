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

        // Keep cheap indexes only. Destination bytes are fetched lazily and cached by URL,
        // so a large destination library does not cause a full media download/index pass.
        val byUrl = destination
            .asSequence()
            .filter { it.src.isNotBlank() }
            .associateBy { normalize(it.src) }
        val byName = destination
            .asSequence()
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
        // One source-content hash -> one destination media. This is the main fast path
        // for packages containing the same image under multiple filenames/references.
        val byContentHash = mutableMapOf<String, ProductImage>()
        // One destination URL -> hash, including failed lookups. This guarantees that a
        // destination image is never downloaded repeatedly during one import.
        val remoteHashCache = mutableMapOf<String, String?>()
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
                    val hash = sha256(bytes)
                    val sourceImage = sourceByFile[file]

                    val exact = sourceImage?.src
                        ?.let { candidateUrl ->
                            byUrl[normalize(candidateUrl)]?.takeIf {
                                remoteHash(it.src, remoteHashCache) == hash
                            }
                        }

                    val cached = byContentHash[hash]

                    // Only inspect same-name destination candidates after the cheap exact-URL
                    // and content-cache paths have failed. Candidate URLs are de-duplicated so
                    // the same destination media can never be fetched twice for one source file.
                    val byFilename = if (exact == null && cached == null) {
                        val wantedName = normalize(fileName(sourceImage?.name ?: file))
                        byName[wantedName]
                            .orEmpty()
                            .asSequence()
                            .distinctBy { normalize(it.src) }
                            .mapNotNull { candidate ->
                                remoteHash(candidate.src, remoteHashCache)
                                    ?.takeIf { it == hash }
                                    ?.let { candidate }
                            }
                            .firstOrNull()
                    } else {
                        null
                    }

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
                                onProgress(
                                    ProductTransferProgress(
                                        "در حال آپلود تصاویر…",
                                        uploaded,
                                        -1,
                                    )
                                )
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
        if (src.isBlank()) return null
        val key = normalize(src)
        if (cache.containsKey(key)) return cache[key]
        val value = remoteBytes(src)?.let(::sha256)
        cache[key] = value
        return value
    }

    private fun remoteBytes(src: String): ByteArray? {
        val connection = try {
            URL(src).openConnection() as HttpURLConnection
        } catch (_: Throwable) {
            return null
        }

        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
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
            connection.disconnect()
        }
    }

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
