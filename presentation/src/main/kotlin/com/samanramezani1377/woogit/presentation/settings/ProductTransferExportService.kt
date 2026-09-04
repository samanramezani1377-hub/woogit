package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.model.ProductType
import com.samanramezani1377.woogit.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream

private const val FORMAT = "woogit-products"
private const val VERSION = 1
private const val PAGE_SIZE = 100
private const val MAX_PRODUCTS = 10_000
private const val MAX_VARIATIONS_PER_PRODUCT = 10_000
private const val MAX_PACKAGE_BYTES = 1_073_741_824L
private const val MAX_ENTRY_BYTES = 50L * 1024L * 1024L
private val transferJson = kotlinx.serialization.json.Json { prettyPrint = false; ignoreUnknownKeys = true; explicitNulls = false }

class ProductTransferExportService(private val d: V1PresentationDependencies, private val resolver: ContentResolver) {
    private val reader = ProductTransferRepositoryReader(d, PAGE_SIZE)

    suspend fun export(storeId: StoreId, destination: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val store = requireStore(storeId)
            val products = reader.products(storeId, onProgress)
            require(products.size <= MAX_PRODUCTS) { "تعداد محصولات از حد مجاز بیشتر است." }
            val allCategories = reader.categories(storeId)
            val usedGlobalIds = products.flatMap { it.attributes }.mapNotNull { it.id?.value }.toSet()
            val globals = reader.attributes(storeId).filter { it.id.value in usedGlobalIds }.map { g -> TransferGlobalAttribute(g.id.value, g.name, g.slug, reader.terms(storeId, g.id).map { TransferTerm(it.id?.value, it.name, it.slug) }) }
            var imageCount = 0
            resolver.openOutputStream(destination)?.use { raw -> CountingOutputStream(raw, MAX_PACKAGE_BYTES).use { counted -> ZipOutputStream(counted).use { zip ->
                val exported = products.mapIndexed { index, product ->
                    onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…", index + 1, products.size))
                    val images = product.images.mapIndexed { imageIndex, image ->
                        val file = "media/p-${product.id.value}-$imageIndex.${transferExt(image.src)}"
                        val bytes = downloadTransferImage(image.src) ?: error("تصویر «${image.name ?: image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                        require(bytes.size.toLong() <= MAX_ENTRY_BYTES); writeTransferEntry(zip, file, bytes); imageCount++
                        TransferImage(image.id?.value, image.src, image.name, image.alt, file)
                    }
                    val variations = if (product.type == ProductType.VARIABLE) {
                        val all = reader.variations(storeId, product.id); require(all.size <= MAX_VARIATIONS_PER_PRODUCT)
                        all.map { variation -> variation.toTransfer { image ->
                            val file = "media/v-${variation.id.value}.${transferExt(image.src)}"; val bytes = downloadTransferImage(image.src) ?: error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                            require(bytes.size.toLong() <= MAX_ENTRY_BYTES); writeTransferEntry(zip, file, bytes); imageCount++; file
                        } }
                    } else emptyList()
                    product.copy(categories = expandCategoryChain(product.categories, allCategories)).toTransfer(images, variations)
                }
                val manifest = ProductTransferManifest(FORMAT, VERSION, ProductTransferFormat.LAYOUT_VERSION, store.baseUrl.trimEnd('/'), SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()), exported.size, imageCount)
                writeTransferEntry(zip, "manifest.json", transferJson.encodeToString(manifest).toByteArray())
                writeTransferEntry(zip, "products.json", transferJson.encodeToString(ProductTransferPackage(manifest, exported, globals)).toByteArray())
            } } } ?: error("امکان ایجاد فایل خروجی وجود ندارد.")
            products.size
        }
    }.onFailure { t -> TechnicalErrorReporter.report("Product Transfer", "ProductTransferExportService.export", "EXPORT", "ساخت فایل خروجی ناموفق بود.", t, "PRODUCT_TRANSFER_EXPORT_ERROR", "storeId=${storeId.value}; destination=$destination") }

    private suspend fun requireStore(id: StoreId) = when (val result = d.getStore(id)) {
        is CoreResult.Success -> result.value
        is CoreResult.Failure -> error("فروشگاه در دسترس نیست.")
    }

    private fun expandCategoryChain(categories: List<IdName>, all: List<IdName>): List<IdName> {
        val byId = all.associateBy { it.id.value }; val out = linkedMapOf<String, IdName>()
        fun add(c: IdName) { if (out.putIfAbsent(c.id.value, c) == null) c.parentId?.let { byId[it.value]?.let(::add) } }
        categories.forEach(::add); return out.values.toList()
    }
}
