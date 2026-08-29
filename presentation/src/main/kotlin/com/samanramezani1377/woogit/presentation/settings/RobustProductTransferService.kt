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
private const val NEW_ID_PLACEHOLDER = "new"

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
    val variationsFailed: Int = 0,
    val imagesFailed: Int = 0,
    val imagesUnused: Int = 0,
    val skuChanged: Int = 0,
    val validationErrors: List<String> = emptyList(),
    val importErrors: List<String> = emptyList(),
)

private data class ReadPackage(
    val manifest: Manifest,
    val products: List<P>,
    val uploadedImages: Map<String, ProductImage>,
)
private data class ValidatedPackage(
    val manifest: Manifest,
    val products: List<P>,
    val mediaNames: Set<String>,
    val invalidProductIds: Set<String>,
    val validationErrors: List<String>,
)
private data class MediaUploadOutcome(
    val images: Map<String, ProductImage>,
    val failed: Int,
    val errors: List<String>,
)

private fun normalize(value: String?): String = value.orEmpty().trim().lowercase(Locale.ROOT)
private fun cleanSku(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun P.matchKey(): String = sha256(listOf(normalize(name), normalize(type), normalize(regular), normalize(sale), normalize(description), normalize(shortDescription)).joinToString("|"))
private fun Product.matchKey(): String = sha256(listOf(normalize(name), type.name.lowercase(Locale.ROOT), normalize(pricing.regular), normalize(pricing.sale), normalize(description), normalize(shortDescription)).joinToString("|"))
private fun nextUniqueSku(original: String?, used: Set<String>): String? { val base = cleanSku(original) ?: return null; var candidate = base; while (normalize(candidate) in used) candidate = "0$candidate"; return candidate }
private fun reserveNewSku(original: String?, used: MutableSet<String>, onChanged: () -> Unit): String? { val unique = nextUniqueSku(original, used) ?: return null; used += normalize(unique); if (!unique.equals(original, ignoreCase = false)) onChanged(); return unique }
private fun releaseSku(sku: String?, used: MutableSet<String>) { sku?.let { used -= normalize(it) } }
private fun validateProduct(x: P): String? = when {
    x.name.isBlank() -> "محصول با شناسه ${x.id} فاقد نام است و وارد نشد."
    x.type.isBlank() -> "محصول «${x.name}» فاقد نوع (type) است و وارد نشد."
    x.status.isBlank() -> "محصول «${x.name}» فاقد وضعیت (status) است و وارد نشد."
    else -> null
}
private class CountingOutputStream(delegate: OutputStream, private val maxBytes: Long) : FilterOutputStream(delegate) {
    var count: Long = 0; private set
    override fun write(b: Int) { check(count + 1 <= maxBytes) { "حجم فایل خروجی بیش از حد مجاز است." }; out.write(b); count++ }
    override fun write(b: ByteArray, off: Int, len: Int) { check(count + len <= maxBytes) { "حجم فایل خروجی بیش از حد مجاز است." }; out.write(b, off, len); count += len }
}

class RobustProductTransferService(private val d: V1PresentationDependencies, private val resolver: ContentResolver) {
    suspend fun export(storeId: StoreId, destination: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val store = when (val r = d.getStore(storeId)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("دریافت اطلاعات فروشگاه ناموفق بود.") }
            val products = allProducts(storeId, onProgress); require(products.size <= MAX_PRODUCTS) { "تعداد محصولات از حد مجاز بیشتر است." }
            var exportedImages = 0
            resolver.openOutputStream(destination)?.use { raw -> CountingOutputStream(raw, MAX_PACKAGE_BYTES).use { counted -> ZipOutputStream(counted).use { zip ->
                val exported = products.mapIndexed { index, product ->
                    onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…", index + 1, products.size))
                    val images = product.images.mapIndexed { imageIndex, image ->
                        val file = "media/p-${product.id.value}-$imageIndex.${ext(image.src)}"
                        val bytes = downloadImage(image.src) ?: error("تصویر «${image.name ?: image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                        require(bytes.size.toLong() <= MAX_ENTRY_BYTES) { "تصویر بیش از حد بزرگ است." }; writeZipEntry(zip, file, bytes); exportedImages++
                        ImageX(image.id?.value, image.src, image.name, image.alt, file)
                    }
                    val variations = if (product.type == ProductType.VARIABLE) {
                        val vars = allVars(storeId, product.id); require(vars.size <= MAX_VARIATIONS_PER_PRODUCT)
                        vars.map { variation -> variation.toX { image -> val file = "media/v-${variation.id.value}.${ext(image.src)}"; val bytes = downloadImage(image.src) ?: error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد."); require(bytes.size.toLong() <= MAX_ENTRY_BYTES); writeZipEntry(zip, file, bytes); exportedImages++; file } }
                    } else emptyList()
                    product.toX(images, variations)
                }
                val manifest = Manifest(source = store.baseUrl.trimEnd('/'), exportedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()), products = exported.size, images = exportedImages)
                writeZipEntry(zip, "manifest.json", transferJson.encodeToString(manifest).toByteArray()); writeZipEntry(zip, "products.json", transferJson.encodeToString(Package(manifest, exported)).toByteArray())
            } } } ?: error("امکان ایجاد فایل خروجی وجود ندارد.")
            products.size
        }
    }

    suspend fun import(storeId: StoreId, source: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): RobustProductTransferResult = withContext(Dispatchers.IO) {
        try {
            val store = when (val r = d.getStore(storeId)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> return@withContext RobustProductTransferResult(failed = 1, errors = listOf("فروشگاه در دسترس نیست.")) }
            val validated = validatePackage(source); val sameStore = validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'), true); val media = uploadMediaPass(storeId, source, onProgress)
            val pack = ReadPackage(validated.manifest, validated.products, media.images); val existing = allProducts(storeId, onProgress)
            val byId = existing.associateBy { it.id.value }; val bySku = existing.mapNotNull { p -> cleanSku(p.sku)?.let { normalize(it) to p } }.toMap(); val byFingerprint = existing.groupBy { it.matchKey() }
            val usedSkuKeys = existing.mapNotNullTo(mutableSetOf()) { cleanSku(it.sku)?.let(::normalize) }
            existing.filter { it.type == ProductType.VARIABLE }.forEach { p -> allVars(storeId, p.id).forEach { v -> cleanSku(v.sku)?.let { usedSkuKeys += normalize(it) } } }
            val categories = allCategories(storeId); val categoryByName = categories.associateBy { normalize(it.name) }
            var created = 0; var updated = 0; var failed = 0; var variationsCreated = 0; var variationsUpdated = 0; var variationsFailed = 0; var skuChanged = 0
            val usedMediaFiles = mutableSetOf<String>(); val errors = mutableListOf<String>(); val importErrors = mutableListOf<String>(); val validationErrors = validated.validationErrors.toMutableList()
            pack.products.forEachIndexed { index, x ->
                onProgress(ProductTransferProgress("در حال وارد کردن محصولات…", index + 1, pack.products.size)); if (x.id in validated.invalidProductIds) { failed++; return@forEachIndexed }
                try {
                    val old = findProductMatch(x, sameStore, byId, bySku, byFingerprint)
                    val images = x.images.mapNotNull { img -> pack.uploadedImages[img.file]?.also { usedMediaFiles += img.file } }
                    val resolvedCategories = x.categories.mapNotNull { cat -> categoryByName[normalize(cat.name)] ?: if (sameStore) IdName(EntityId(cat.id), cat.name) else null }
                    if (!sameStore && resolvedCategories.size < x.categories.size) errors += "${x.name}: برخی دسته‌بندی‌ها در فروشگاه مقصد وجود نداشتند و حذف شدند."
                    val attrs = x.attributes.map { attr -> Attribute(if (sameStore) attr.id?.let(::EntityId) else null, attr.name, attr.visible, attr.variation, attr.options) }
                    val reservedSku = if (old == null) reserveNewSku(x.sku, usedSkuKeys) { skuChanged++ } else null
                    val product = if (old == null) x.toDomain(EntityId(NEW_ID_PLACEHOLDER), images, resolvedCategories, attrs).copy(sku = reservedSku) else x.toDomain(old.id, images, resolvedCategories, attrs)
                    val saved = if (old == null) d.createProduct(storeId, product) else d.updateProduct(storeId, old.id, product.copy(id = old.id))
                    val savedProduct = when (saved) { is CoreResult.Success -> { if (old == null) created++ else updated++; saved.value }; is CoreResult.Failure -> { failed++; importErrors += "${x.name}: ${saved.error}"; errors += importErrors.last(); releaseSku(reservedSku, usedSkuKeys); return@forEachIndexed } }
                    val existingVars = allVars(storeId, savedProduct.id); if (old != null) existingVars.forEach { cleanSku(it.sku)?.let { usedSkuKeys += normalize(it) } }
                    x.variations.forEach { vv ->
                        val oldVariation = if (sameStore) existingVars.firstOrNull { it.id.value == vv.id } else cleanSku(vv.sku)?.let { sku -> existingVars.firstOrNull { cleanSku(it.sku)?.let(::normalize) == normalize(sku) } }
                        val image = vv.image?.let { img -> pack.uploadedImages[img.file]?.also { usedMediaFiles += img.file } }
                        val reservedVariationSku = if (oldVariation == null) reserveNewSku(vv.sku, usedSkuKeys) { skuChanged++ } else null
                        val variation = if (oldVariation == null) vv.toDomain(savedProduct.id, EntityId(NEW_ID_PLACEHOLDER), image).copy(sku = reservedVariationSku) else vv.toDomain(savedProduct.id, oldVariation.id, image)
                        when (val r = if (oldVariation == null) d.createVariation(storeId, variation) else d.updateVariation(storeId, savedProduct.id, oldVariation.id, variation.copy(id = oldVariation.id))) {
                            is CoreResult.Success -> if (oldVariation == null) variationsCreated++ else variationsUpdated++
                            is CoreResult.Failure -> { variationsFailed++; importErrors += "${x.name}: variation ${vv.sku ?: vv.id} وارد نشد: ${r.error}"; errors += importErrors.last(); releaseSku(reservedVariationSku, usedSkuKeys) }
                        }
                    }
                } catch (t: Throwable) { failed++; importErrors += "${x.name}: ${t.message ?: "خطای نامشخص"}"; errors += importErrors.last() }
            }
            val imagesUnused = (media.images.keys - usedMediaFiles).size
            RobustProductTransferResult(created, updated, failed, media.images.size, variationsCreated, variationsUpdated, (validationErrors + media.errors + errors).distinct().take(50), variationsFailed, media.failed, imagesUnused, skuChanged, validationErrors.distinct().take(50), importErrors.distinct().take(50))
        } catch (t: Throwable) { RobustProductTransferResult(failed = 1, errors = listOf(t.message ?: "خواندن فایل ناموفق بود.")) }
    }

    private fun findProductMatch(x: P, sameStore: Boolean, byId: Map<String, Product>, bySku: Map<String, Product>, byFingerprint: Map<String, List<Product>>): Product? { if (sameStore) byId[x.id]?.let { return it }; cleanSku(x.sku)?.let { bySku[normalize(it)]?.let { p -> return p } }; return byFingerprint[x.matchKey()].orEmpty().singleOrNull() }

    private fun validatePackage(uri: Uri): ValidatedPackage = with(run {
        var manifest: Manifest? = null; var products: List<P>? = null; val mediaNames = linkedSetOf<String>(); var totalRead = 0L
        resolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip -> while (true) { val entry = zip.nextEntry ?: break; if (entry.isDirectory) continue; val name = entry.name; require(!name.contains("..") && !name.startsWith("/")) { "مسیر نامعتبر داخل فایل WooGit." }; when (name) {
            "manifest.json" -> { val bytes = readLimited(zip, MAX_ENTRY_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) }; manifest = transferJson.decodeFromString(bytes.toString(Charsets.UTF_8)) }
            "products.json" -> { val bytes = readLimited(zip, MAX_PRODUCT_JSON_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) }; val p = transferJson.decodeFromString<Package>(bytes.toString(Charsets.UTF_8)); manifest = manifest ?: p.manifest; products = p.products }
            else -> if (name.startsWith("media/")) { readLimited(zip, MAX_ENTRY_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) }; mediaNames += name }
        } } } } ?: error("فایل قابل خواندن نیست.")
        val m = requireNotNull(manifest) { "manifest.json در فایل وجود ندارد." }; val p = requireNotNull(products) { "products.json در فایل وجود ندارد." }; require(m.format == FORMAT) { "فرمت فایل WooGit معتبر نیست." }; require(m.version <= VERSION) { "نسخه فایل پشتیبانی نمی‌شود." }; require(m.products == p.size) { "تعداد محصولات فایل با manifest سازگار نیست." }; require(p.size <= MAX_PRODUCTS) { "تعداد محصولات فایل بیش از حد مجاز است." }; require(m.images == mediaNames.size) { "برخی تصاویر فایل قابل وارد کردن نیستند." }
        val validationErrors = mutableListOf<String>(); val invalid = mutableSetOf<String>(); val skuSeen = mutableSetOf<String>(); p.forEach { x -> validateProduct(x)?.let { validationErrors += it; invalid += x.id }; cleanSku(x.sku)?.let { if (!skuSeen.add(normalize(it))) validationErrors += "SKU تکراری داخل فایل: $it" }; x.images.forEach { img -> if (img.file !in mediaNames) validationErrors += "«${x.name}»: تصویر ${img.file} در بسته موجود نیست." }; x.variations.forEach { v -> cleanSku(v.sku)?.let { if (!skuSeen.add(normalize(it))) validationErrors += "SKU تکراری داخل فایل: $it" }; v.image?.let { img -> if (img.file !in mediaNames) validationErrors += "«${x.name}»: تصویر Variation ${img.file} در بسته موجود نیست." } } }
        ValidatedPackage(m, p, mediaNames, invalid, validationErrors)
    })

    private suspend fun uploadMediaPass(storeId: StoreId, uri: Uri, onProgress: (ProductTransferProgress) -> Unit): MediaUploadOutcome = withContext(Dispatchers.IO) { val uploaded = linkedMapOf<String, ProductImage>(); val errors = mutableListOf<String>(); var failedCount = 0; resolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip -> while (true) { val entry = zip.nextEntry ?: break; if (entry.isDirectory) continue; val name = entry.name; if (!name.startsWith("media/")) continue; val bytes = readLimited(zip, MAX_ENTRY_BYTES) {}; when (val r = d.uploadMedia(storeId, name.substringAfterLast('/'), bytes, mime(name))) { is CoreResult.Success -> { uploaded[name] = r.value; onProgress(ProductTransferProgress("در حال آپلود تصاویر…", uploaded.size, -1)) }; is CoreResult.Failure -> { failedCount++; errors += "آپلود رسانه $name ناموفق بود: ${r.error}" } } } } } ?: error("فایل قابل خواندن نیست."); MediaUploadOutcome(uploaded, failedCount, errors) }
    private fun readLimited(input: ZipInputStream, maxBytes: Long, onBytes: (Long) -> Unit): ByteArray { val out = ByteArrayOutputStream(); val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var total = 0L; while (true) { val read = input.read(buffer); if (read <= 0) break; total += read; require(total <= maxBytes) { "یکی از فایل‌های داخل بسته بیش از حد بزرگ است." }; onBytes(read.toLong()); out.write(buffer, 0, read) }; return out.toByteArray() }
    private fun writeZipEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }
    private suspend fun allProducts(s: StoreId, progress: (ProductTransferProgress) -> Unit): List<Product> { val result = mutableListOf<Product>(); var page = 1; while (true) { val batch = when (val r = d.getProducts(s, page, PAGE_SIZE, null)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("دریافت محصولات ناموفق بود: ${r.error}") }; if (batch.isEmpty()) break; result += batch; progress(ProductTransferProgress("در حال دریافت محصولات…", result.size, result.size)); if (batch.size < PAGE_SIZE) break; page++ }; return result.distinctBy { it.id.value } }
    private suspend fun allVars(s: StoreId, id: EntityId): List<Variation> { val result = mutableListOf<Variation>(); var page = 1; while (true) { val batch = when (val r = d.getVariations(s, id, page, PAGE_SIZE)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("دریافت Variationهای محصول ناموفق بود: ${r.error}") }; if (batch.isEmpty()) break; result += batch; if (batch.size < PAGE_SIZE) break; page++ }; return result.distinctBy { it.id.value } }
    private suspend fun allCategories(s: StoreId): List<IdName> { val result = mutableListOf<IdName>(); var page = 1; while (true) { val batch = when (val r = d.getProductCategories(s, page, PAGE_SIZE, null)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> break }; if (batch.isEmpty()) break; result += batch; if (batch.size < PAGE_SIZE) break; page++ }; return result.distinctBy { it.id.value } }
    private fun downloadImage(src: String): ByteArray? = try { val connection = URL(src).openConnection() as HttpURLConnection; connection.connectTimeout = 15_000; connection.readTimeout = 30_000; connection.instanceFollowRedirects = true; connection.inputStream.use { stream -> val out = ByteArrayOutputStream(); val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var total = 0L; while (true) { val read = stream.read(buffer); if (read <= 0) break; total += read; if (total > MAX_ENTRY_BYTES) return null; out.write(buffer, 0, read) }; out.toByteArray() }.also { connection.disconnect() } } catch (_: Throwable) { null }
    private fun ext(s: String): String = s.substringBefore('?').substringAfterLast('.', "jpg").lowercase(Locale.ROOT).let { if (it in setOf("jpg", "jpeg", "png", "webp", "gif")) it else "jpg" }
    private fun mime(s: String): String = when (ext(s)) { "png" -> "image/png"; "webp" -> "image/webp"; "gif" -> "image/gif"; else -> "image/jpeg" }
}

private fun Product.toX(images: List<ImageX>, variations: List<V>) = P(id = id.value, name = name, sku = sku, description = description, shortDescription = shortDescription, status = status.name, type = type.name, regular = pricing.regular, sale = pricing.sale, onSale = pricing.onSale, quantity = stock?.quantity, stockStatus = stock?.status?.name, manageStock = stock?.manageStock ?: false, categories = categories.map { IdNameX(it.id.value, it.name) }, attributes = attributes.map { AttrX(it.id?.value, it.name, it.visible, it.variation, it.options) }, images = images, variations = variations, modifiedAt = modifiedAt)
private fun P.toDomain(id: EntityId, images: List<ProductImage>, categories: List<IdName>, attributes: List<Attribute>) = Product(id = id, name = name, sku = sku, description = description, shortDescription = shortDescription, status = runCatching { ProductStatus.valueOf(status) }.getOrDefault(ProductStatus.DRAFT), type = runCatching { ProductType.valueOf(type) }.getOrDefault(ProductType.SIMPLE), pricing = Pricing(regular, sale, onSale), stock = if (quantity != null || stockStatus != null || manageStock) Stock(quantity, runCatching { StockStatus.valueOf(stockStatus ?: StockStatus.IN_STOCK.name) }.getOrDefault(StockStatus.IN_STOCK), manageStock) else null, images = images, categories = categories, attributes = attributes, modifiedAt = modifiedAt)
private fun Variation.toX(imageWriter: (ProductImage) -> String) = V(id = id.value, sku = sku, regular = pricing.regular, sale = pricing.sale, onSale = pricing.onSale, quantity = stock?.quantity, stockStatus = stock?.status?.name, manageStock = stock?.manageStock ?: false, attributes = attributes.map { VA(it.name, it.option) }, image = image?.let { ImageX(it.id?.value, it.src, it.name, it.alt, imageWriter(it)) }, modifiedAt = modifiedAt?.toString())
private fun V.toDomain(productId: EntityId, id: EntityId, image: ProductImage?) = Variation(id = id, productId = productId, attributes = attributes.map { VariationAttribute(it.name, it.option) }, pricing = Pricing(regular, sale, onSale), stock = if (quantity != null || stockStatus != null || manageStock) Stock(quantity, runCatching { StockStatus.valueOf(stockStatus ?: StockStatus.IN_STOCK.name) }.getOrDefault(StockStatus.IN_STOCK), manageStock) else null, sku = sku, image = image, modifiedAt = modifiedAt?.let { kotlinx.datetime.Instant.parse(it) })
