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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val FORMAT = "woogit-products"
private const val VERSION = 1
private const val PAGE_SIZE = 100
private const val MAX_PRODUCTS = 10_000
private const val MAX_VARIATIONS_PER_PRODUCT = 10_000
private const val MAX_PACKAGE_BYTES = 1_073_741_824L
private const val MAX_ENTRY_BYTES = 50L * 1024L * 1024L
private const val MAX_PRODUCT_JSON_BYTES = 100L * 1024L * 1024L
private const val NEW_ID_PLACEHOLDER = "new"
private val transferJson = Json { prettyPrint = false; ignoreUnknownKeys = true; explicitNulls = false }

class RobustProductTransferService(private val d: V1PresentationDependencies, private val resolver: ContentResolver) {
    suspend fun export(storeId: StoreId, destination: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val store = requireStore(storeId)
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
                                require(bytes.size.toLong() <= MAX_ENTRY_BYTES) { "تصویر بیش از حد بزرگ است." }
                                writeZipEntry(zip, file, bytes); exportedImages++
                                TransferImage(image.id?.value, image.src, image.name, image.alt, file)
                            }
                            val variations = if (product.type == ProductType.VARIABLE) {
                                val vars = allVars(storeId, product.id)
                                require(vars.size <= MAX_VARIATIONS_PER_PRODUCT)
                                vars.map { variation -> variation.toTransfer { image ->
                                    val file = "media/v-${variation.id.value}.${ext(image.src)}"
                                    val bytes = downloadImage(image.src) ?: error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                                    require(bytes.size.toLong() <= MAX_ENTRY_BYTES)
                                    writeZipEntry(zip, file, bytes); exportedImages++; file
                                } }
                            } else emptyList()
                            product.toTransfer(images, variations)
                        }
                        val manifest = ProductTransferManifest(FORMAT, VERSION, store.baseUrl.trimEnd('/'), SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()), exported.size, exportedImages)
                        writeZipEntry(zip, "manifest.json", transferJson.encodeToString(manifest).toByteArray())
                        writeZipEntry(zip, "products.json", transferJson.encodeToString(ProductTransferPackage(manifest, exported)).toByteArray())
                    }
                }
            } ?: error("امکان ایجاد فایل خروجی وجود ندارد.")
            products.size
        }
    }

    suspend fun import(storeId: StoreId, source: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): RobustProductTransferResult = withContext(Dispatchers.IO) {
        try {
            val store = requireStore(storeId)
            val validated = validatePackage(source)
            if (validated.validationErrors.isNotEmpty()) {
                return@withContext RobustProductTransferResult(failed = validated.invalidProductIds.size, validationErrors = validated.validationErrors, errors = validated.validationErrors)
            }
            val sameStore = validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'), true)
            val media = uploadMediaPass(storeId, source, onProgress)
            val existing = allProducts(storeId, onProgress)
            val byId = existing.associateBy { it.id.value }
            val bySku = existing.mapNotNull { p -> cleanSku(p.sku)?.let { normalize(it) to p } }.toMap()
            val byFingerprint = existing.groupBy { it.matchKey() }
            val usedSkuKeys = existing.mapNotNullTo(mutableSetOf()) { cleanSku(it.sku)?.let(::normalize) }
            existing.filter { it.type == ProductType.VARIABLE }.forEach { p -> allVars(storeId, p.id).forEach { v -> cleanSku(v.sku)?.let { usedSkuKeys += normalize(it) } } }
            val categories = allCategories(storeId)
            val categoryByKey = categories.associateBy { normalize(it.name) }
            var created = 0; var updated = 0; var failed = 0; var variationsCreated = 0; var variationsUpdated = 0; var variationsFailed = 0; var skuChanged = 0
            val usedMediaFiles = mutableSetOf<String>(); val errors = mutableListOf<String>(); val importErrors = mutableListOf<String>()
            packLoop@ for ((index, x) in validated.products.withIndex()) {
                onProgress(ProductTransferProgress("در حال وارد کردن محصولات…", index + 1, validated.products.size))
                try {
                    val old = findProductMatch(x, sameStore, byId, bySku, byFingerprint)
                    val images = x.images.mapNotNull { img -> media.images[img.file]?.also { usedMediaFiles += img.file } }
                    val resolvedCategories = x.categories.mapNotNull { cat -> categoryByKey[normalize(cat.name)] ?: if (sameStore) IdName(EntityId(cat.id), cat.name) else null }
                    val attrs = x.attributes.map { attr -> Attribute(if (sameStore) attr.id?.let(::EntityId) else null, attr.name, attr.visible, attr.variation, attr.options) }
                    val reservedSku = if (old == null) reserveNewSku(x.sku, usedSkuKeys) { skuChanged++ } else null
                    val product = if (old == null) x.toDomain(EntityId(NEW_ID_PLACEHOLDER), images, resolvedCategories, attrs).copy(sku = reservedSku) else x.toDomain(old.id, images, resolvedCategories, attrs)
                    val saved = if (old == null) d.createProduct(storeId, product) else d.updateProduct(storeId, old.id, product.copy(id = old.id))
                    val savedProduct = when (saved) {
                        is CoreResult.Success -> { if (old == null) created++ else updated++; saved.value }
                        is CoreResult.Failure -> { failed++; importErrors += "${x.name}: ${saved.error}"; releaseSku(reservedSku, usedSkuKeys); continue@packLoop }
                    }
                    val existingVars = allVars(storeId, savedProduct.id)
                    x.variations.forEach { vv ->
                        val oldVariation = when {
                            sameStore -> existingVars.firstOrNull { it.id.value == vv.id }
                            else -> cleanSku(vv.sku)?.let { sku -> existingVars.firstOrNull { cleanSku(it.sku)?.let(::normalize) == normalize(sku) } }
                                ?: existingVars.firstOrNull { it.attributeKey() == vv.attributeKey() }
                        }
                        val image = vv.image?.let { img -> media.images[img.file]?.also { usedMediaFiles += img.file } }
                        val reservedVariationSku = if (oldVariation == null) reserveNewSku(vv.sku, usedSkuKeys) { skuChanged++ } else null
                        val variation = if (oldVariation == null) vv.toDomain(savedProduct.id, EntityId(NEW_ID_PLACEHOLDER), image).copy(sku = reservedVariationSku) else vv.toDomain(savedProduct.id, oldVariation.id, image)
                        when (val r = if (oldVariation == null) d.createVariation(storeId, variation) else d.updateVariation(storeId, savedProduct.id, oldVariation.id, variation.copy(id = oldVariation.id))) {
                            is CoreResult.Success -> if (oldVariation == null) variationsCreated++ else variationsUpdated++
                            is CoreResult.Failure -> { variationsFailed++; importErrors += "${x.name}: variation ${vv.sku ?: vv.id} وارد نشد: ${r.error}"; releaseSku(reservedVariationSku, usedSkuKeys) }
                        }
                    }
                } catch (t: Throwable) { failed++; importErrors += "${x.name}: ${t.message ?: "خطای نامشخص"}" }
            }
            RobustProductTransferResult(created, updated, failed, media.uploaded, variationsCreated, variationsUpdated, (media.errors + errors + importErrors).distinct().take(50), variationsFailed, media.failed, (media.images.keys - usedMediaFiles).size, skuChanged, emptyList(), importErrors.distinct().take(50))
        } catch (t: Throwable) { RobustProductTransferResult(failed = 1, errors = listOf(t.message ?: "خواندن فایل ناموفق بود.")) }
    }

    private suspend fun requireStore(id: StoreId) = when (val r = d.getStore(id)) { is CoreResult.Success -> r.value; is CoreResult.Failure -> error("فروشگاه در دسترس نیست.") }

    private fun findProductMatch(x: TransferProduct, sameStore: Boolean, byId: Map<String, Product>, bySku: Map<String, Product>, byFingerprint: Map<String, List<Product>>): Product? {
        if (sameStore) byId[x.id]?.let { return it }
        cleanSku(x.sku)?.let { bySku[normalize(it)]?.let { p -> return p } }
        return byFingerprint[x.matchKey()].orEmpty().singleOrNull()
    }

    private fun validatePackage(uri: Uri): TransferValidatedPackage {
        var manifest: ProductTransferManifest? = null; var products: List<TransferProduct>? = null; val mediaNames = linkedSetOf<String>(); var totalRead = 0L
        resolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; if (entry.isDirectory) continue; val name = entry.name
                require(!name.startsWith("/") && !name.contains("..")) { "مسیر نامعتبر داخل فایل WooGit." }
                when (name) {
                    "manifest.json" -> { val bytes = readLimited(zip, MAX_ENTRY_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) }; manifest = transferJson.decodeFromString(bytes.toString(Charsets.UTF_8)) }
                    "products.json" -> { val bytes = readLimited(zip, MAX_PRODUCT_JSON_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) }; val p = transferJson.decodeFromString<ProductTransferPackage>(bytes.toString(Charsets.UTF_8)); manifest = manifest ?: p.manifest; products = p.products }
                    else -> if (name.startsWith("media/")) { readLimited(zip, MAX_ENTRY_BYTES) { totalRead += it; require(totalRead <= MAX_PACKAGE_BYTES) }; mediaNames += name }
                }
            }
        }} ?: error("فایل قابل خواندن نیست.")
        val m = requireNotNull(manifest) { "manifest.json در فایل وجود ندارد." }; val p = requireNotNull(products) { "products.json در فایل وجود ندارد." }
        require(m.format == FORMAT) { "فرمت فایل WooGit معتبر نیست." }; require(m.version <= VERSION) { "نسخه فایل پشتیبانی نمی‌شود." }; require(m.products == p.size) { "تعداد محصولات فایل با manifest سازگار نیست." }; require(p.size <= MAX_PRODUCTS) { "تعداد محصولات فایل بیش از حد مجاز است." }; require(m.images == mediaNames.size) { "برخی تصاویر فایل قابل وارد کردن نیستند." }
        val errors = mutableListOf<String>(); val invalid = mutableSetOf<String>(); val skuSeen = mutableSetOf<String>()
        p.forEach { x -> validateProduct(x)?.let { errors += it; invalid += x.id }; cleanSku(x.sku)?.let { if (!skuSeen.add(normalize(it))) errors += "SKU تکراری داخل فایل: $it" }; x.images.forEach { if (it.file !in mediaNames) errors += "«${x.name}»: تصویر ${it.file} در بسته موجود نیست." }; x.variations.forEach { v -> cleanSku(v.sku)?.let { if (!skuSeen.add(normalize(it))) errors += "SKU تکراری داخل فایل: $it" }; v.image?.let { if (it.file !in mediaNames) errors += "«${x.name}»: تصویر Variation ${it.file} در بسته موجود نیست." } } }
        return TransferValidatedPackage(m, p, mediaNames, invalid, errors.distinct())
    }

    private suspend fun uploadMediaPass(storeId: StoreId, uri: Uri, onProgress: (ProductTransferProgress) -> Unit): TransferMediaOutcome = withContext(Dispatchers.IO) {
        val destinationMedia = allMedia(storeId); val byUrl = destinationMedia.associateBy { normalize(it.src) }; val byName = destinationMedia.groupBy { normalize(it.name ?: it.src.substringBefore('?').substringAfterLast('/')) }
        val uploaded = linkedMapOf<String, ProductImage>(); val errors = mutableListOf<String>(); var failed = 0; var uploadedCount = 0
        resolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; val name = entry?.name ?: break; if (entry.isDirectory || !name.startsWith("media/")) continue
                val bytes = readLimited(zip, MAX_ENTRY_BYTES) {}
                val fileName = name.substringAfterLast('/'); val sourceImage = findSourceImage(uri = null, fileName = name)
                val exact = sourceImage?.src?.let { byUrl[normalize(it)] }
                val candidate = exact ?: byName[normalize(fileName)].orEmpty().firstOrNull()
                val reused = if (candidate != null && (exact != null || sameBytes(candidate.src, bytes))) candidate else null
                if (reused != null) uploaded[name] = reused else when (val r = d.uploadMedia(storeId, fileName, bytes, mime(name))) {
                    is CoreResult.Success -> { uploaded[name] = r.value; uploadedCount++; onProgress(ProductTransferProgress("در حال آپلود تصاویر…", uploadedCount, -1)) }
                    is CoreResult.Failure -> { failed++; errors += "آپلود رسانه $name ناموفق بود: ${r.error}" }
                }
            }
        }} ?: error("فایل قابل خواندن نیست.")
        TransferMediaOutcome(uploaded, failed, errors, uploadedCount)
    }

    private fun findSourceImage(uri: Uri?, fileName: String): TransferImage? = null
    private fun sameBytes(src: String, bytes: ByteArray): Boolean = try { downloadImage(src)?.contentEquals(bytes) == true } catch (_: Throwable) { false }
    private fun readLimited(input: ZipInputStream, maxBytes: Long, onBytes: (Long) -> Unit): ByteArray { val out=ByteArrayOutputStream(); val buffer=ByteArray(DEFAULT_BUFFER_SIZE); var total=0L; while(true){ val n=input.read(buffer); if(n<=0) break; total+=n; require(total<=maxBytes){"یکی از فایل‌های داخل بسته بیش از حد بزرگ است."}; onBytes(n.toLong()); out.write(buffer,0,n) }; return out.toByteArray() }
    private fun writeZipEntry(zip: ZipOutputStream,path:String,bytes:ByteArray){zip.putNextEntry(ZipEntry(path));zip.write(bytes);zip.closeEntry()}
    private suspend fun allProducts(s:StoreId,progress:(ProductTransferProgress)->Unit):List<Product>{val result=mutableListOf<Product>();var page=1;while(true){val batch=when(val r=d.getProducts(s,page,PAGE_SIZE,null)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت محصولات ناموفق بود: ${r.error}")};if(batch.isEmpty())break;result+=batch;progress(ProductTransferProgress("در حال دریافت محصولات…",result.size,result.size));if(batch.size<PAGE_SIZE)break;page++};return result.distinctBy{it.id.value}}
    private suspend fun allVars(s:StoreId,id:EntityId):List<Variation>{val result=mutableListOf<Variation>();var page=1;while(true){val batch=when(val r=d.getVariations(s,id,page,PAGE_SIZE)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت Variationهای محصول ناموفق بود: ${r.error}")};if(batch.isEmpty())break;result+=batch;if(batch.size<PAGE_SIZE)break;page++};return result.distinctBy{it.id.value}}
    private suspend fun allCategories(s:StoreId):List<IdName>{val result=mutableListOf<IdName>();var page=1;while(true){val batch=when(val r=d.getProductCategories(s,page,PAGE_SIZE,null)){is CoreResult.Success->r.value;is CoreResult.Failure->break};if(batch.isEmpty())break;result+=batch;if(batch.size<PAGE_SIZE)break;page++};return result.distinctBy{it.id.value}}
    private suspend fun allMedia(s:StoreId):List<ProductImage>{val result=mutableListOf<ProductImage>();var page=1;while(true){val batch=when(val r=d.getMedia(s,page,PAGE_SIZE,null)){is CoreResult.Success->r.value;is CoreResult.Failure->break};if(batch.isEmpty())break;result+=batch;if(batch.size<PAGE_SIZE)break;page++};return result.distinctBy{it.id.value}}
    private fun downloadImage(src:String):ByteArray?=try{val c=URL(src).openConnection() as HttpURLConnection;c.connectTimeout=15_000;c.readTimeout=30_000;c.instanceFollowRedirects=true;c.inputStream.use{stream->val out=ByteArrayOutputStream();val buffer=ByteArray(DEFAULT_BUFFER_SIZE);var total=0L;while(true){val n=stream.read(buffer);if(n<=0)break;total+=n;if(total>MAX_ENTRY_BYTES)return null;out.write(buffer,0,n)};out.toByteArray()}.also{c.disconnect()}}catch(_:Throwable){null}
    private fun ext(s:String)=s.substringBefore('?').substringAfterLast('.','jpg').lowercase(Locale.ROOT).let{if(it in setOf("jpg","jpeg","png","webp","gif"))it else "jpg"}
    private fun mime(s:String)=when(ext(s)){"png"->"image/png";"webp"->"image/webp";"gif"->"image/gif";else->"image/jpeg"}
    private class CountingOutputStream(delegate:OutputStream,private val maxBytes:Long):FilterOutputStream(delegate){var count=0L;private set;override fun write(b:Int){check(count+1<=maxBytes);out.write(b);count++};override fun write(b:ByteArray,off:Int,len:Int){check(count+len<=maxBytes);out.write(b,off,len);count+=len}}
}
