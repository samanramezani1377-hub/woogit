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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val transferJson = Json { prettyPrint = false; ignoreUnknownKeys = true; explicitNulls = false }
private const val FORMAT = "woogit-products"
private const val VERSION = 1
private const val PAGE_SIZE = 100
private const val MAX_PRODUCTS = 10_000
private const val MAX_VARIATIONS_PER_PRODUCT = 10_000
private const val MAX_PACKAGE_BYTES = 1_073_741_824L
private const val MAX_ENTRY_BYTES = 50L * 1024L * 1024L
private const val MAX_PRODUCT_JSON_BYTES = 100L * 1024L * 1024L

@Serializable private data class Manifest(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val source: String,
    val exportedAt: String,
    val products: Int,
    val images: Int,
)

@Serializable private data class Package(val manifest: Manifest, val products: List<P>)
@Serializable private data class P(
    val id: String,
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val status: String,
    val type: String,
    val regular: String? = null,
    val sale: String? = null,
    val onSale: Boolean,
    val quantity: Double? = null,
    val stockStatus: String? = null,
    val manageStock: Boolean,
    val categories: List<IdNameX> = emptyList(),
    val attributes: List<AttrX> = emptyList(),
    val images: List<ImageX> = emptyList(),
    val variations: List<V> = emptyList(),
    val modifiedAt: String? = null,
)

@Serializable private data class V(
    val id: String,
    val sku: String? = null,
    val regular: String? = null,
    val sale: String? = null,
    val onSale: Boolean,
    val quantity: Double? = null,
    val stockStatus: String? = null,
    val manageStock: Boolean,
    val attributes: List<VA> = emptyList(),
    val image: ImageX? = null,
    val modifiedAt: String? = null,
)

@Serializable private data class VA(val name: String, val option: String)
@Serializable private data class IdNameX(val id: String, val name: String)
@Serializable private data class AttrX(
    val id: String? = null,
    val name: String,
    val visible: Boolean,
    val variation: Boolean,
    val options: List<String>,
)
@Serializable private data class ImageX(
    val id: String? = null,
    val src: String,
    val name: String? = null,
    val alt: String? = null,
    val file: String,
)

data class RobustProductTransferResult(
    val created: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val imagesUploaded: Int = 0,
    val variationsCreated: Int = 0,
    val variationsUpdated: Int = 0,
    val errors: List<String> = emptyList(),
)

private data class ReadPackage(
    val manifest: Manifest,
    val products: List<P>,
    val uploadedImages: Map<String, ProductImage>,
)

private fun normalize(value: String?): String = value.orEmpty().trim().lowercase(Locale.ROOT)
private fun cleanSku(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray())
    .joinToString("") { "%02x".format(it) }

private fun P.matchKey(): String = sha256(
    listOf(normalize(name), normalize(type), normalize(regular), normalize(sale), normalize(description), normalize(shortDescription)).joinToString("|")
)

private fun Product.matchKey(): String = sha256(
    listOf(normalize(name), type.name.lowercase(Locale.ROOT), normalize(pricing.regular), normalize(pricing.sale), normalize(description), normalize(shortDescription)).joinToString("|")
)

private class CountingOutputStream(delegate: OutputStream, private val maxBytes: Long) : FilterOutputStream(delegate) {
    var count: Long = 0
        private set
    override fun write(b: Int) {
        check(count + 1 <= maxBytes) { "حجم فایل خروجی بیش از حد مجاز است." }
        out.write(b); count++
    }
    override fun write(b: ByteArray, off: Int, len: Int) {
        check(count + len <= maxBytes) { "حجم فایل خروجی بیش از حد مجاز است." }
        out.write(b, off, len); count += len
    }
}

class RobustProductTransferService(
    private val d: V1PresentationDependencies,
    private val resolver: ContentResolver,
) {
    suspend fun export(storeId: StoreId, destination: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val store = when (val r = d.getStore(storeId)) {
                is CoreResult.Success -> r.value
                is CoreResult.Failure -> error("دریافت اطلاعات فروشگاه ناموفق بود.")
            }
            val products = allProducts(storeId, onProgress)
            require(products.size <= MAX_PRODUCTS) { "تعداد محصولات از حد مجاز بیشتر است." }
            var exportedImages = 0
            resolver.openOutputStream(destination)?.use { raw ->
                CountingOutputStream(raw, MAX_PACKAGE_BYTES).use { counted ->
                    ZipOutputStream(counted).use { zip ->
                        val exported = products.mapIndexed { index, product ->
                            onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…", index + 1, products.size))
                            val images = product.images.mapIndexed { imageIndex, image ->
                                val file = "media/p-${product.id.value}-$imageIndex.${ext(image.src)}"
                                val bytes = downloadImage(image.src) ?: error("تصویر «${image.name ?: image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                                require(bytes.size.toLong() <= MAX_ENTRY_BYTES) { "تصویر ${image.name ?: image.src} بیش از حد بزرگ است." }
                                writeZipEntry(zip, file, bytes)
                                exportedImages++
                                ImageX(image.id?.value, image.src, image.name, image.alt, file)
                            }
                            val variations = if (product.type == ProductType.VARIABLE) {
                                val vars = allVars(storeId, product.id)
                                require(vars.size <= MAX_VARIATIONS_PER_PRODUCT) { "تعداد Variationهای محصول «${product.name}» بیش از حد مجاز است." }
                                vars.map { variation ->
                                    variation.toX { image ->
                                        val file = "media/v-${variation.id.value}.${ext(image.src)}"
                                        val bytes = downloadImage(image.src) ?: error("تصویر Variation «${image.name ?: image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                                        require(bytes.size.toLong() <= MAX_ENTRY_BYTES) { "تصویر Variation بیش از حد بزرگ است." }
                                        writeZipEntry(zip, file, bytes)
                                        exportedImages++
                                        file
                                    }
                                }
                            } else emptyList()
                            product.toX(images, variations)
                        }
                        val manifest = Manifest(
                            source = store.baseUrl.trimEnd('/'),
                            exportedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()),
                            products = exported.size,
                            images = exportedImages,
                        )
                        writeZipEntry(zip, "manifest.json", transferJson.encodeToString(manifest).toByteArray())
                        writeZipEntry(zip, "products.json", transferJson.encodeToString(Package(manifest, exported)).toByteArray())
                    }
                }
            } ?: error("امکان ایجاد فایل خروجی وجود ندارد.")
            products.size
        }
    }

    suspend fun import(storeId: StoreId, source: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): RobustProductTransferResult = withContext(Dispatchers.IO) {
        try {
            val store = when (val r = d.getStore(storeId)) {
                is CoreResult.Success -> r.value
                is CoreResult.Failure -> return@withContext RobustProductTransferResult(failed = 1, errors = listOf("فروشگاه در دسترس نیست."))
            }
            val pack = readAndUploadMedia(storeId, source, onProgress)
            require(pack.manifest.format == FORMAT) { "فرمت فایل WooGit معتبر نیست." }
            require(pack.manifest.version <= VERSION) { "نسخه فایل پشتیبانی نمی‌شود." }
            require(pack.products.size <= MAX_PRODUCTS) { "تعداد محصولات فایل بیش از حد مجاز است." }
            val sameStore = pack.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'), true)
            val existing = allProducts(storeId, onProgress)
            val byId = existing.associateBy { it.id.value }
            val bySku = existing.mapNotNull { p -> cleanSku(p.sku)?.let { it to p } }.toMap()
            val byFingerprint = existing.groupBy { it.matchKey() }
            val categories = allCategories(storeId)
            val categoryByName = categories.associateBy { normalize(it.name) }
            var created = 0; var updated = 0; var failed = 0; var imagesUploaded = pack.uploadedImages.size
            var variationsCreated = 0; var variationsUpdated = 0
            val errors = mutableListOf<String>()
            pack.products.forEachIndexed { index, x ->
                onProgress(ProductTransferProgress("در حال وارد کردن محصولات…", index + 1, pack.products.size))
                try {
                    val old = findProductMatch(x, sameStore, byId, bySku, byFingerprint)
                    val images = x.images.mapNotNull { pack.uploadedImages[it.file] }
                    val resolvedCategories = x.categories.mapNotNull { cat ->
                        categoryByName[normalize(cat.name)] ?: if (sameStore) IdName(EntityId(cat.id), cat.name) else null
                    }
                    if (!sameStore && resolvedCategories.size < x.categories.size) errors += "${x.name}: برخی دسته‌بندی‌ها در فروشگاه مقصد وجود نداشتند و حذف شدند."
                    val attrs = x.attributes.map { attr -> Attribute(if (sameStore) attr.id?.let(::EntityId) else null, attr.name, attr.visible, attr.variation, attr.options) }
                    val product = x.toDomain(old?.id, images, resolvedCategories, attrs)
                    val saved = if (old == null) d.createProduct(storeId, product) else d.updateProduct(storeId, old.id, product.copy(id = old.id))
                    val savedProduct = when (saved) {
                        is CoreResult.Success -> { if (old == null) created++ else updated++; saved.value }
                        is CoreResult.Failure -> { failed++; errors += "${x.name}: ${saved.error}"; return@forEachIndexed }
                    }
                    val existingVars = allVars(storeId, savedProduct.id)
                    x.variations.forEach { vv ->
                        val oldVariation = findVariationMatch(vv, sameStore, existingVars)
                        val image = vv.image?.let { pack.uploadedImages[it.file] }
                        val variation = vv.toDomain(savedProduct.id, oldVariation?.id, image)
                        when (val r = if (oldVariation == null) d.createVariation(storeId, variation) else d.updateVariation(storeId, savedProduct.id, oldVariation.id, variation.copy(id = oldVariation.id))) {
                            is CoreResult.Success -> if (oldVariation == null) variationsCreated++ else variationsUpdated++
                            is CoreResult.Failure -> errors += "${x.name}: variation ${vv.sku ?: vv.id} وارد نشد: ${r.error}"
                        }
                    }
                } catch (t: Throwable) {
                    failed++; errors += "${x.name}: ${t.message ?: "خطای نامشخص"}"
                }
            }
            RobustProductTransferResult(created, updated, failed, imagesUploaded, variationsCreated, variationsUpdated, errors.distinct().take(50))
        } catch (t: Throwable) {
            RobustProductTransferResult(failed = 1, errors = listOf(t.message ?: "خواندن فایل ناموفق بود."))
        }
    }

    private fun findProductMatch(x: P, sameStore: Boolean, byId: Map<String, Product>, bySku: Map<String, Product>, byFingerprint: Map<String, List<Product>>): Product? {
        if (sameStore) byId[x.id]?.let { return it }
        cleanSku(x.sku)?.let { bySku[it]?.let { p -> return p } }
        return byFingerprint[x.matchKey()].orEmpty().singleOrNull()
    }

    private fun findVariationMatch(x: V, sameStore: Boolean, existing: List<Variation>): Variation? {
        if (sameStore) existing.firstOrNull { it.id.value == x.id }?.let { return it }
        cleanSku(x.sku)?.let { sku -> existing.firstOrNull { cleanSku(it.sku) == sku }?.let { return it } }
        return null
    }

    private suspend fun readAndUploadMedia(storeId: StoreId, uri: Uri, onProgress: (ProductTransferProgress) -> Unit): ReadPackage = withContext(Dispatchers.IO) {
        var manifest: Manifest? = null; var products: List<P>? = null
        val uploaded = linkedMapOf<String, ProductImage>(); var totalRead = 0L
        resolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name
                require(!name.contains("..") && !name.startsWith("/")) { "مسیر نامعتبر داخل فایل WooGit." }
                val bytes = readLimited(zip, if (name == "products.json") MAX_PRODUCT_JSON_BYTES else MAX_ENTRY_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) { "حجم فایل WooGit بیش از حد مجاز است." } }
                when (name) {
                    "manifest.json" -> manifest = transferJson.decodeFromString(bytes.toString(Charsets.UTF_8))
                    "products.json" -> { val p = transferJson.decodeFromString<Package>(bytes.toString(Charsets.UTF_8)); manifest = manifest ?: p.manifest; products = p.products }
                    else -> if (name.startsWith("media/")) {
                        val image = when (val r = d.uploadMedia(storeId, name.substringAfterLast('/'), bytes, mime(name))) {
                            is CoreResult.Success -> r.value
                            is CoreResult.Failure -> throw IllegalStateException("آپلود رسانه $name ناموفق بود: ${r.error}")
                        }
                        uploaded[name] = image
                        onProgress(ProductTransferProgress("در حال آپلود تصاویر…", uploaded.size, -1))
                    }
                }
            }
        }} ?: error("فایل قابل خواندن نیست.")
        val m = requireNotNull(manifest) { "manifest.json در فایل وجود ندارد." }
        val p = requireNotNull(products) { "products.json در فایل وجود ندارد." }
        require(m.products == p.size) { "تعداد محصولات فایل با manifest سازگار نیست." }
        require(m.images == uploaded.size) { "برخی تصاویر فایل قابل وارد کردن نیستند." }
        ReadPackage(m, p, uploaded)
    }

    private fun readLimited(input: ZipInputStream, maxBytes: Long, onBytes: (Long) -> Unit): ByteArray {
        val out = ByteArrayOutputStream(); val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var total = 0L
        while (true) { val read = input.read(buffer); if (read <= 0) break; total += read; require(total <= maxBytes) { "یکی از فایل‌های داخل بسته بیش از حد بزرگ است." }; onBytes(read.toLong()); out.write(buffer, 0, read) }
        return out.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }

    private suspend fun allProducts(s: StoreId, progress: (ProductTransferProgress) -> Unit): List<Product> {
        val result = mutableListOf<Product>(); var page = 1
        while (true) {
            val batch = when (val r = d.getProducts(s, page, PAGE_SIZE, null)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("دریافت محصولات ناموفق بود: ${r.error}") }
            if (batch.isEmpty()) break
            result += batch; progress(ProductTransferProgress("در حال دریافت محصولات…", result.size, result.size)); if (batch.size < PAGE_SIZE) break; page++
        }
        return result.distinctBy { it.id.value }
    }

    private suspend fun allVars(s: StoreId, id: EntityId): List<Variation> {
        val result = mutableListOf<Variation>(); var page = 1
        while (true) {
            val batch = when (val r = d.getVariations(s, id, page, PAGE_SIZE)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("دریافت Variationهای محصول ناموفق بود: ${r.error}") }
            if (batch.isEmpty()) break
            result += batch; if (batch.size < PAGE_SIZE) break; page++
        }
        return result.distinctBy { it.id.value }
    }

    private suspend fun allCategories(s: StoreId): List<IdName> {
        val result = mutableListOf<IdName>(); var page = 1
        while (true) {
            val batch = when (val r = d.getProductCategories(s, page, PAGE_SIZE, null)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> break }
            if (batch.isEmpty()) break
            result += batch; if (batch.size < PAGE_SIZE) break; page++
        }
        return result.distinctBy { it.id.value }
    }

    private fun downloadImage(src: String): ByteArray? = try {
        val connection = URL(src).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000; connection.readTimeout = 30_000; connection.instanceFollowRedirects = true
        connection.inputStream.use { stream ->
            val out = ByteArrayOutputStream(); val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var total = 0L
            while (true) { val read = stream.read(buffer); if (read <= 0) break; total += read; if (total > MAX_ENTRY_BYTES) return null; out.write(buffer, 0, read) }
            out.toByteArray()
        }.also { connection.disconnect() }
    } catch (_: Throwable) { null }

    private fun ext(s: String): String = s.substringBefore('?').substringAfterLast('.', "jpg").lowercase(Locale.ROOT).let { if (it in setOf("jpg", "jpeg", "png", "webp", "gif")) it else "jpg" }
    private fun mime(s: String): String = when (ext(s)) { "png" -> "image/png"; "webp" -> "image/webp"; "gif" -> "image/gif"; else -> "image/jpeg" }
}

private fun Product.toX(images: List<ImageX>, variations: List<V>) = P(
    id = id.value, name = name, sku = sku, description = description, shortDescription = shortDescription,
    status = status.name, type = type.name, regular = pricing.regular, sale = pricing.sale, onSale = pricing.onSale,
    quantity = stock?.quantity, stockStatus = stock?.status?.name, manageStock = stock?.manageStock ?: false,
    categories = categories.map { IdNameX(it.id.value, it.name) },
    attributes = attributes.map { AttrX(it.id?.value, it.name, it.visible, it.variation, it.options) },
    images = images, variations = variations, modifiedAt = modifiedAt,
)

private fun P.toDomain(id: EntityId?, images: List<ProductImage>, categories: List<IdName>, attributes: List<Attribute>) = Product(
    id = id ?: EntityId(this.id), name = name, sku = sku, description = description, shortDescription = shortDescription,
    status = runCatching { ProductStatus.valueOf(status) }.getOrDefault(ProductStatus.DRAFT),
    type = runCatching { ProductType.valueOf(type) }.getOrDefault(ProductType.SIMPLE),
    pricing = Pricing(regular, sale, onSale),
    stock = if (quantity != null || stockStatus != null || manageStock) Stock(quantity, runCatching { StockStatus.valueOf(stockStatus ?: StockStatus.IN_STOCK.name) }.getOrDefault(StockStatus.IN_STOCK), manageStock) else null,
    images = images, categories = categories, attributes = attributes, modifiedAt = modifiedAt,
)

private fun Variation.toX(imageWriter: (ProductImage) -> String) = V(
    id = id.value, sku = sku, regular = pricing.regular, sale = pricing.sale, onSale = pricing.onSale,
    quantity = stock?.quantity, stockStatus = stock?.status?.name, manageStock = stock?.manageStock ?: false,
    attributes = attributes.map { VA(it.name, it.option) },
    image = image?.let { ImageX(it.id?.value, it.src, it.name, it.alt, imageWriter(it)) }, modifiedAt = modifiedAt?.toString(),
)

private fun V.toDomain(productId: EntityId, id: EntityId?, image: ProductImage?) = Variation(
    id = id ?: EntityId(this.id), productId = productId,
    attributes = attributes.map { VariationAttribute(it.name, it.option) }, pricing = Pricing(regular, sale, onSale),
    stock = if (quantity != null || stockStatus != null || manageStock) Stock(quantity, runCatching { StockStatus.valueOf(stockStatus ?: StockStatus.IN_STOCK.name) }.getOrDefault(StockStatus.IN_STOCK), manageStock) else null,
    sku = sku, image = image, modifiedAt = modifiedAt?.let { runCatching { kotlinx.datetime.Instant.parse(it) }.getOrNull() },
)
