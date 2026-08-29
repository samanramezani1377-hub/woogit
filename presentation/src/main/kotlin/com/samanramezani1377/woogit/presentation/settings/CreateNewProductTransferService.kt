package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

private val createNewJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private const val CREATE_NEW_FORMAT = "woogit-products"
private const val CREATE_NEW_VERSION = 1
private const val CREATE_NEW_MAX_PRODUCTS = 10_000
private const val CREATE_NEW_MAX_ENTRY_BYTES = 50L * 1024L * 1024L
private const val CREATE_NEW_MAX_PACKAGE_BYTES = 1_073_741_824L

@Serializable private data class CreatePackage(val manifest: CreateManifest, val products: List<CreateProduct>)
@Serializable private data class CreateManifest(val format: String = CREATE_NEW_FORMAT, val version: Int = CREATE_NEW_VERSION, val source: String = "", val exportedAt: String = "", val products: Int = 0, val images: Int = 0)
@Serializable private data class CreateProduct(val id: String, val name: String, val sku: String? = null, val description: String? = null, val shortDescription: String? = null, val status: String, val type: String, val regular: String? = null, val sale: String? = null, val onSale: Boolean, val quantity: Double? = null, val stockStatus: String? = null, val manageStock: Boolean, val categories: List<CreateIdName> = emptyList(), val attributes: List<CreateAttribute> = emptyList(), val images: List<CreateImage> = emptyList(), val variations: List<CreateVariation> = emptyList())
@Serializable private data class CreateVariation(val id: String, val sku: String? = null, val regular: String? = null, val sale: String? = null, val onSale: Boolean, val quantity: Double? = null, val stockStatus: String? = null, val manageStock: Boolean, val attributes: List<CreateVariationAttribute> = emptyList(), val image: CreateImage? = null)
@Serializable private data class CreateVariationAttribute(val name: String, val option: String)
@Serializable private data class CreateIdName(val id: String, val name: String)
@Serializable private data class CreateAttribute(val id: String? = null, val name: String, val visible: Boolean, val variation: Boolean, val options: List<String>)
@Serializable private data class CreateImage(val id: String? = null, val src: String, val name: String? = null, val alt: String? = null, val file: String)
private data class CreateReadPackage(val manifest: CreateManifest, val products: List<CreateProduct>, val media: Map<String, ByteArray>)

class CreateNewProductTransferService(private val d: V1PresentationDependencies, private val resolver: ContentResolver) {
    suspend fun import(storeId: StoreId, source: Uri, createAsDraft: Boolean = false, onProgress: (ProductTransferProgress) -> Unit = {}): RobustProductTransferResult = withContext(Dispatchers.IO) {
        try {
            val pack = readPackage(source, onProgress)
            require(pack.manifest.format == CREATE_NEW_FORMAT) { "فرمت فایل WooGit معتبر نیست." }
            require(pack.manifest.version <= CREATE_NEW_VERSION) { "نسخه فایل پشتیبانی نمی‌شود." }
            require(pack.products.size <= CREATE_NEW_MAX_PRODUCTS) { "تعداد محصولات فایل بیش از حد مجاز است." }
            val existingProducts = allProducts(storeId)
            val usedSkus = existingProducts.mapNotNull { normalizeSku(it.sku) }.toMutableSet()
            val categories = allCategories(storeId).toMutableList()
            val categoryByName = categories.associateBy { normalize(it.name) }.toMutableMap()
            val uploaded = linkedMapOf<String, ProductImage>()
            var created = 0; var failed = 0; var imagesUploaded = 0; var variationsCreated = 0
            val errors = mutableListOf<String>()
            pack.media.forEach { (file, bytes) ->
                when (val r = d.uploadMedia(storeId, file.substringAfterLast('/'), bytes, mime(file))) {
                    is CoreResult.Success -> { uploaded[file] = r.value; imagesUploaded++ }
                    is CoreResult.Failure -> errors += "تصویر $file وارد نشد: ${r.error}"
                }
            }
            pack.products.forEachIndexed { index, x ->
                onProgress(ProductTransferProgress("در حال ایجاد محصولات جدید…", index + 1, pack.products.size))
                try {
                    val images = x.images.mapNotNull { uploaded[it.file] }
                    val resolvedCategories = x.categories.mapNotNull { cat ->
                        categoryByName[normalize(cat.name)] ?: run {
                            when (val r = d.createProductCategory(storeId, cat.name)) {
                                is CoreResult.Success -> { categories += r.value; categoryByName[normalize(cat.name)] = r.value; r.value }
                                is CoreResult.Failure -> { errors += "${x.name}: دسته‌بندی «${cat.name}» ساخته نشد: ${r.error}"; null }
                            }
                        }
                    }
                    val attributes = x.attributes.map { Attribute(null, it.name, it.visible, it.variation, it.options) }
                    val uniqueSku = nextUniqueSku(x.sku, usedSkus)
                    uniqueSku?.let { usedSkus += it }
                    val product = x.toDomain(images, resolvedCategories, attributes, uniqueSku).let { if (createAsDraft) it.copy(status = ProductStatus.DRAFT) else it }
                    val saved = when (val r = d.createProduct(storeId, product)) {
                        is CoreResult.Success -> { created++; r.value }
                        is CoreResult.Failure -> { failed++; errors += "${x.name}: ${r.error}"; return@forEachIndexed }
                    }
                    x.variations.forEach { v ->
                        val image = v.image?.let { uploaded[it.file] }
                        val variationSku = nextUniqueSku(v.sku, usedSkus)
                        variationSku?.let { usedSkus += it }
                        when (val r = d.createVariation(storeId, v.toDomain(saved.id, image, variationSku))) {
                            is CoreResult.Success -> variationsCreated++
                            is CoreResult.Failure -> errors += "${x.name}: variation ${v.sku ?: v.id} ایجاد نشد: ${r.error}"
                        }
                    }
                } catch (t: Throwable) { failed++; errors += "${x.name}: ${t.message ?: "خطای نامشخص"}" }
            }
            RobustProductTransferResult(created, 0, failed, imagesUploaded, variationsCreated, 0, errors.distinct().take(50))
        } catch (t: Throwable) { RobustProductTransferResult(failed = 1, errors = listOf(t.message ?: "خواندن فایل ناموفق بود.")) }
    }

    private fun nextUniqueSku(original: String?, used: MutableSet<String>): String? {
        val base = normalizeSku(original) ?: return null
        var candidate = base
        while (!used.add(candidate)) candidate = "0$candidate"
        used.remove(candidate)
        return candidate
    }

    private fun normalizeSku(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }?.lowercase(Locale.ROOT)
    private suspend fun readPackage(uri: Uri, onProgress: (ProductTransferProgress) -> Unit): CreateReadPackage = withContext(Dispatchers.IO) {
        var manifest: CreateManifest? = null; var products: List<CreateProduct>? = null; val media = linkedMapOf<String, ByteArray>(); var totalBytes = 0L
        resolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name
                require(!name.contains("..") && !name.startsWith('/')) { "مسیر نامعتبر داخل فایل WooGit." }
                val bytes = readLimited(zip, if (name == "products.json") 100L * 1024L * 1024L else CREATE_NEW_MAX_ENTRY_BYTES) { totalBytes += it; require(totalBytes <= CREATE_NEW_MAX_PACKAGE_BYTES) { "حجم فایل WooGit بیش از حد مجاز است." } }
                when (name) { "manifest.json" -> manifest = createNewJson.decodeFromString(bytes.toString(Charsets.UTF_8)); "products.json" -> { val p = createNewJson.decodeFromString<CreatePackage>(bytes.toString(Charsets.UTF_8)); manifest = manifest ?: p.manifest; products = p.products }; else -> if (name.startsWith("media/")) media[name] = bytes }
            }
        }} ?: error("فایل قابل خواندن نیست.")
        val m = requireNotNull(manifest) { "manifest.json در فایل وجود ندارد." }; val p = requireNotNull(products) { "products.json در فایل وجود ندارد." }
        require(m.products == p.size) { "تعداد محصولات فایل با manifest سازگار نیست." }; require(m.images == media.size) { "برخی تصاویر فایل قابل وارد کردن نیستند." }
        CreateReadPackage(m, p, media)
    }
    private fun readLimited(input: ZipInputStream, maxBytes: Long, onBytes: (Long) -> Unit): ByteArray { val out = ByteArrayOutputStream(); val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var total = 0L; while (true) { val read = input.read(buffer); if (read <= 0) break; total += read; require(total <= maxBytes) { "یکی از فایل‌های داخل بسته بیش از حد بزرگ است." }; onBytes(read.toLong()); out.write(buffer, 0, read) }; return out.toByteArray() }
    private suspend fun allProducts(s: StoreId): List<Product> { val result = mutableListOf<Product>(); var page = 1; while (true) { val batch = when (val r = d.getProducts(s, page, 100, null)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("دریافت محصولات ناموفق بود: ${r.error}") }; if (batch.isEmpty()) break; result += batch; if (batch.size < 100) break; page++ }; return result.distinctBy { it.id.value } }
    private suspend fun allCategories(storeId: StoreId): List<IdName> { val result = mutableListOf<IdName>(); var page = 1; while (true) { val batch = when (val r = d.getProductCategories(storeId, page, 100, null)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> break }; if (batch.isEmpty()) break; result += batch; if (batch.size < 100) break; page++ }; return result.distinctBy { it.id.value } }
    private fun normalize(value: String?): String = value.orEmpty().trim().lowercase(Locale.ROOT)
    private fun ext(value: String): String = value.substringBefore('?').substringAfterLast('.', "jpg").lowercase(Locale.ROOT).let { if (it in setOf("jpg", "jpeg", "png", "webp", "gif")) it else "jpg" }
    private fun mime(value: String): String = when (ext(value)) { "png" -> "image/png"; "webp" -> "image/webp"; "gif" -> "image/gif"; else -> "image/jpeg" }
}

private fun CreateProduct.toDomain(images: List<ProductImage>, categories: List<IdName>, attributes: List<Attribute>, sku: String? = this.sku) = Product(id = EntityId(id), name = name, sku = sku, description = description, shortDescription = shortDescription, status = runCatching { ProductStatus.valueOf(status) }.getOrDefault(ProductStatus.DRAFT), type = runCatching { ProductType.valueOf(type) }.getOrDefault(ProductType.SIMPLE), pricing = Pricing(regular, sale, onSale), stock = if (quantity != null || stockStatus != null || manageStock) Stock(quantity, runCatching { StockStatus.valueOf(stockStatus ?: StockStatus.IN_STOCK.name) }.getOrDefault(StockStatus.IN_STOCK), manageStock) else null, images = images, categories = categories, attributes = attributes, modifiedAt = null)
private fun CreateVariation.toDomain(productId: EntityId, image: ProductImage?, sku: String? = this.sku) = Variation(id = EntityId(id), productId = productId, attributes = attributes.map { VariationAttribute(it.name, it.option) }, pricing = Pricing(regular, sale, onSale), stock = if (quantity != null || stockStatus != null || manageStock) Stock(quantity, runCatching { StockStatus.valueOf(stockStatus ?: StockStatus.IN_STOCK.name) }.getOrDefault(StockStatus.IN_STOCK), manageStock) else null, sku = sku, image = image, modifiedAt = null)
