package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream

private const val FORMAT = "woogit-products"
private const val VERSION = 1
private const val PAGE_SIZE = 100
private const val IMPORT_CONCURRENCY = 5
private const val MAX_PRODUCTS = 10_000
private const val MAX_VARIATIONS_PER_PRODUCT = 10_000
private const val MAX_PACKAGE_BYTES = 1_073_741_824L
private const val MAX_ENTRY_BYTES = 50L * 1024L * 1024L
private const val NEW_ID_PLACEHOLDER = "new"
private val transferJson = Json { prettyPrint = false; ignoreUnknownKeys = true; explicitNulls = false }

class RobustProductTransferService(private val d: V1PresentationDependencies, private val resolver: ContentResolver) {
    private val reader = ProductTransferRepositoryReader(d, PAGE_SIZE)
    private val media = ProductTransferMedia(d, resolver)

    suspend fun export(storeId: StoreId, destination: Uri, onProgress: (ProductTransferProgress) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val store = requireStore(storeId)
            val products = reader.products(storeId, onProgress)
            require(products.size <= MAX_PRODUCTS) { "تعداد محصولات از حد مجاز بیشتر است." }
            val allCategories = reader.categories(storeId)
            val usedGlobalIds = products.flatMap { it.attributes }.mapNotNull { it.id?.value }.toSet()
            val globals = reader.attributes(storeId)
                .filter { it.id.value in usedGlobalIds }
                .map { g -> TransferGlobalAttribute(g.id.value, g.name, g.slug, reader.terms(storeId, g.id).map { TransferTerm(it.id?.value, it.name, it.slug) }) }
            var imageCount = 0
            resolver.openOutputStream(destination)?.use { raw ->
                CountingOutputStream(raw, MAX_PACKAGE_BYTES).use { counted ->
                    ZipOutputStream(counted).use { zip ->
                        val exported = products.mapIndexed { index, product ->
                            onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…", index + 1, products.size))
                            val images = product.images.mapIndexed { imageIndex, image ->
                                val file = "media/p-${product.id.value}-$imageIndex.${transferExt(image.src)}"
                                val bytes = downloadTransferImage(image.src) ?: error("تصویر «${image.name ?: image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                                require(bytes.size.toLong() <= MAX_ENTRY_BYTES)
                                writeTransferEntry(zip, file, bytes)
                                imageCount++
                                TransferImage(image.id?.value, image.src, image.name, image.alt, file)
                            }
                            val variations = if (product.type == ProductType.VARIABLE) {
                                val all = reader.variations(storeId, product.id)
                                require(all.size <= MAX_VARIATIONS_PER_PRODUCT) { "تعداد Variationهای محصول بیش از حد مجاز است." }
                                all.map { variation ->
                                    variation.toTransfer { image ->
                                        val file = "media/v-${variation.id.value}.${transferExt(image.src)}"
                                        val bytes = downloadTransferImage(image.src) ?: error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد.")
                                        require(bytes.size.toLong() <= MAX_ENTRY_BYTES)
                                        writeTransferEntry(zip, file, bytes)
                                        imageCount++
                                        file
                                    }
                                }
                            } else emptyList()
                            product.copy(categories = expandCategoryChain(product.categories, allCategories)).toTransfer(images, variations)
                        }
                        val manifest = ProductTransferManifest(
                            FORMAT, VERSION, ProductTransferFormat.LAYOUT_VERSION, store.baseUrl.trimEnd('/'),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()),
                            exported.size, imageCount
                        )
                        writeTransferEntry(zip, "manifest.json", transferJson.encodeToString(manifest).toByteArray())
                        writeTransferEntry(zip, "products.json", transferJson.encodeToString(ProductTransferPackage(manifest, exported, globals)).toByteArray())
                    }
                }
            } ?: error("امکان ایجاد فایل خروجی وجود ندارد.")
            products.size
        }
    }.onFailure { t ->
        TechnicalErrorReporter.report(
            feature = "Product Transfer", location = "RobustProductTransferService.export", operation = "EXPORT",
            userMessage = "ساخت فایل خروجی ناموفق بود.", throwable = t, type = "PRODUCT_TRANSFER_EXPORT_ERROR",
            details = "storeId=${storeId.value}; destination=$destination"
        )
    }

    suspend fun import(storeId: StoreId, source: Uri, mode: ProductImportMode = ProductImportMode.UPDATE_EXISTING, onProgress: (ProductTransferProgress) -> Unit = {}): RobustProductTransferResult = withContext(Dispatchers.IO) {
        try {
            requireStore(storeId)
            val validated = validateTransferPackage(resolver, source, transferJson)
            if (validated.validationErrors.isNotEmpty()) {
                validated.validationErrors.forEach { message ->
                    TechnicalErrorReporter.reportHandled("Product Transfer", "RobustProductTransferService.validate", "IMPORT_VALIDATE", "فایل انتقال محصولات معتبر نیست.", message, "PRODUCT_TRANSFER_VALIDATION_ERROR", "storeId=${storeId.value}; source=$source")
                }
                return@withContext RobustProductTransferResult(failed = validated.invalidProductIds.size, errors = validated.validationErrors, validationErrors = validated.validationErrors)
            }
            val store = requireStore(storeId)
            val sameStore = validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'), true)
            val mediaOutcome = try {
                media.upload(storeId, source, validated.products, onProgress)
            } catch (t: Throwable) {
                TechnicalErrorReporter.report("Product Transfer", "RobustProductTransferService.import.media", "IMPORT_MEDIA", "پردازش تصاویر انتقال ناموفق بود؛ ورود محصولات ادامه پیدا می‌کند.", t, "PRODUCT_TRANSFER_MEDIA_EXCEPTION", "storeId=${storeId.value}; source=$source")
                TransferMediaOutcome(emptyMap(), 0, listOf(t.message ?: "خطای پردازش تصاویر"), 0)
            }
            val existing = reader.products(storeId, onProgress)
            val byId = existing.associateBy { it.id.value }
            val bySku = existing.groupBy { cleanSku(it.sku)?.let(::normalize) }.mapNotNull { (k, v) -> k?.let { key -> v.singleOrNull()?.let { key to it } } }.toMap()
            val byFingerprint = existing.groupBy { it.matchKey() }
            val usedSku = existing.mapNotNullTo(mutableSetOf()) { cleanSku(it.sku)?.let(::normalize) }
            existing.filter { it.type == ProductType.VARIABLE }.forEach { product ->
                reader.variations(storeId, product.id).forEach { variation -> cleanSku(variation.sku)?.let { usedSku += normalize(it) } }
            }
            val destinationCategories = reader.categories(storeId)
            val sourceCategories = validated.products.flatMap { it.categories }.distinctBy { it.id }
            val categoryMap = resolveCategories(storeId, sourceCategories, destinationCategories, true)
            val globalMap = resolveGlobalAttributes(storeId, validated.globalAttributes, mode == ProductImportMode.UPDATE_EXISTING && sameStore)
            val created = AtomicInteger(0)
            val updated = AtomicInteger(0)
            val drafted = AtomicInteger(0)
            val failed = AtomicInteger(0)
            val variationsCreated = AtomicInteger(0)
            val variationsUpdated = AtomicInteger(0)
            val variationsFailed = AtomicInteger(0)
            val skuChanged = AtomicInteger(0)
            val publishedUnexpectedly = AtomicInteger(0)
            val categoriesCreated = categoryMap.created
            val categoriesResolved = categoryMap.resolved
            val attributesCreated = globalMap.created
            val attributesResolved = globalMap.resolved
            val termsCreated = globalMap.termsCreated
            val termsResolved = globalMap.termsResolved
            val usedMedia = ConcurrentHashMap.newKeySet<String>()
            val errors = ConcurrentLinkedQueue<String>()
            val importErrors = ConcurrentLinkedQueue<String>()
            val skuMutex = Mutex()

            coroutineScope {
                validated.products.mapIndexed { index, x ->
                    async(Dispatchers.IO.limitedParallelism(IMPORT_CONCURRENCY)) {
                        try {
                onProgress(ProductTransferProgress("در حال وارد کردن محصولات…", index + 1, validated.products.size))
                var productCountedAsSuccess = false
                var reservedSku: String? = null
                try {
                    val old = if (mode == ProductImportMode.UPDATE_EXISTING) findProductMatch(x, sameStore, byId, bySku, byFingerprint) else null
                    val images = x.images.mapNotNull { image -> mediaOutcome.images[image.file]?.also { usedMedia += image.file } }
                    val categories = x.categories.mapNotNull { categoryMap.items[it.id] }
                    if (categories.size < x.categories.size) errors += "${x.name}: برخی دسته‌بندی‌ها در مقصد Resolve نشدند."
                    val attributes = x.attributes.map { a ->
                        Attribute(a.id?.let { globalMap.items[it] ?: if (mode == ProductImportMode.UPDATE_EXISTING && sameStore) EntityId(it) else null }, a.name, a.visible, a.variation, a.options)
                    }
                    reservedSku = if (old == null) skuMutex.withLock { reserveNewSku(x.sku, usedSku) { skuChanged.incrementAndGet() } } else null
                    var product = x.toDomain(if (old == null) EntityId(NEW_ID_PLACEHOLDER) else old.id, images, categories, attributes).copy(sku = if (old == null) reservedSku else x.sku)
                    if (mode == ProductImportMode.CREATE_NEW_DRAFT) product = product.copy(status = ProductStatus.DRAFT)
                    val saved = if (old == null) d.createProduct(storeId, product) else d.updateProduct(storeId, old.id, product)
                    val savedProduct = when (saved) {
                        is CoreResult.Success -> {
                            if (old == null) { created.incrementAndGet(); if (mode == ProductImportMode.CREATE_NEW_DRAFT) drafted.incrementAndGet() } else updated.incrementAndGet()
                            productCountedAsSuccess = true
                            saved.value
                        }
                        is CoreResult.Failure -> {
                            importErrors += "${x.name}: ${saved.error}"
                            TechnicalErrorReporter.reportHandled("Product Transfer", "RobustProductTransferService.import.product", "CREATE_OR_UPDATE_PRODUCT", "انتقال محصول ناموفق بود.", saved.error.toString(), "PRODUCT_TRANSFER_PRODUCT_ERROR", "product=${x.name}; sourceId=${x.id}; mode=$mode")
                            null
                        }
                    }
                    if (savedProduct == null) return@async
                    var finalProduct = savedProduct
                    if (old == null && mode == ProductImportMode.CREATE_NEW_DRAFT && savedProduct.status != ProductStatus.DRAFT) {
                        val pendingProduct = product.copy(id = savedProduct.id, status = ProductStatus.PENDING)
                        when (val pendingResult = d.updateProduct(storeId, savedProduct.id, pendingProduct)) {
                            is CoreResult.Success -> {
                                finalProduct = pendingResult.value
                                if (finalProduct.status == ProductStatus.DRAFT) drafted.incrementAndGet()
                                if (finalProduct.status == ProductStatus.PUBLISHED) {
                                    publishedUnexpectedly.incrementAndGet()
                                    importErrors.add("${x.name}: پیش‌نویس اعمال نشد و Pending نیز اعمال نشد؛ محصول منتشر شد.")
                                }
                            }
                            is CoreResult.Failure -> {
                                val observed = runCatching { d.getProduct(storeId, savedProduct.id) }.getOrNull()
                                val observedProduct = (observed as? CoreResult.Success)?.value
                                finalProduct = observedProduct ?: savedProduct
                                if (observedProduct?.status == ProductStatus.PUBLISHED || finalProduct.status == ProductStatus.PUBLISHED) {
                                    publishedUnexpectedly.incrementAndGet()
                                    importErrors.add("${x.name}: پیش‌نویس و Pending اعمال نشد؛ محصول منتشر شد.")
                                } else {
                                    importErrors.add("${x.name}: پیش‌نویس اعمال نشد؛ تلاش برای Pending نیز ناموفق بود (${pendingResult.error}).")
                                }
                            }
                        }
                    }
                    val existingVariations = try {
                        reader.variations(storeId, finalProduct.id)
                    } catch (t: Throwable) {
                        variationsFailed.addAndGet(x.variations.size)
                        importErrors += "${x.name}: خواندن Variationها ناموفق بود: ${t.message ?: "خطای نامشخص"}"
                        TechnicalErrorReporter.report("Product Transfer", "RobustProductTransferService.import.variations.read", "READ_VARIATIONS", "خواندن Variationهای محصول ناموفق بود.", t, "PRODUCT_TRANSFER_VARIATION_READ_ERROR", "product=${x.name}; destinationId=${savedProduct.id.value}")
                        emptyList()
                    }
                    x.variations.forEach { sourceVariation ->
                        var reservedVariationSku: String? = null
                        var variationCommitted = false
                        try {
                            val oldVariation = if (old == null) null else if (sameStore) existingVariations.firstOrNull { it.id.value == sourceVariation.id } ?: findVariationByContent(existingVariations, sourceVariation) else findVariationByContent(existingVariations, sourceVariation)
                            val image = sourceVariation.image?.let { mediaOutcome.images[it.file]?.also { usedMedia += sourceVariation.image.file } }
                            reservedVariationSku = if (oldVariation == null) skuMutex.withLock { reserveNewSku(sourceVariation.sku, usedSku) { skuChanged.incrementAndGet() } } else null
                            val variationId = oldVariation?.id ?: EntityId(NEW_ID_PLACEHOLDER)
                            val variation = sourceVariation.toDomain(productId = finalProduct.id, id = variationId, image = image).copy(sku = if (oldVariation == null) reservedVariationSku else sourceVariation.sku)
                            when (val result = if (oldVariation == null) d.createVariation(storeId, variation) else d.updateVariation(storeId, finalProduct.id, oldVariation.id, variation)) {
                                is CoreResult.Success -> { if (oldVariation == null) variationsCreated.incrementAndGet() else variationsUpdated.incrementAndGet(); variationCommitted = true }
                                is CoreResult.Failure -> {
                                    variationsFailed.incrementAndGet()
                                    importErrors += "${x.name}: variation ${sourceVariation.sku ?: sourceVariation.id} وارد نشد: ${result.error}"
                                    TechnicalErrorReporter.reportHandled("Product Transfer", "RobustProductTransferService.import.variation", "CREATE_OR_UPDATE_VARIATION", "انتقال Variation ناموفق بود.", result.error.toString(), "PRODUCT_TRANSFER_VARIATION_ERROR", "product=${x.name}; variation=${sourceVariation.id}; sku=${sourceVariation.sku}")
                                }
                            }
                        } catch (t: Throwable) {
                            variationsFailed.incrementAndGet()
                            importErrors += "${x.name}: variation ${sourceVariation.sku ?: sourceVariation.id} با خطای فنی مواجه شد: ${t.message ?: "خطای نامشخص"}"
                            TechnicalErrorReporter.report("Product Transfer", "RobustProductTransferService.import.variation", "IMPORT_VARIATION", "انتقال Variation ناموفق بود؛ Variationهای بعدی ادامه پیدا می‌کنند.", t, "PRODUCT_TRANSFER_VARIATION_EXCEPTION", "product=${x.name}; variation=${sourceVariation.id}; sku=${sourceVariation.sku}")
                        } finally {
                            if (!variationCommitted) releaseSku(reservedVariationSku, usedSku)
                        }
                    }
                } catch (t: Throwable) {
                    if (!productCountedAsSuccess) failed.incrementAndGet()
                    importErrors += "${x.name}: ${t.message ?: "خطای نامشخص"}"
                    TechnicalErrorReporter.report("Product Transfer", "RobustProductTransferService.import.product", "IMPORT_PRODUCT", "انتقال محصول ناموفق بود؛ محصولات بعدی ادامه پیدا می‌کنند.", t, "PRODUCT_TRANSFER_PRODUCT_EXCEPTION", "product=${x.name}; sourceId=${x.id}; mode=$mode")
                } finally {
                    if (!productCountedAsSuccess) skuMutex.withLock { releaseSku(reservedSku, usedSku) }
                }
                        }
                    }
                }.awaitAll()
            }
            RobustProductTransferResult(created.get(), updated.get(), failed.get(), mediaOutcome.uploaded, variationsCreated.get(), variationsUpdated.get(), (mediaOutcome.errors + errors + importErrors).distinct().take(50), variationsFailed.get(), mediaOutcome.failed, (mediaOutcome.images.keys - usedMedia).size, skuChanged.get(), emptyList(), importErrors.distinct().take(50), drafted.get(), categoriesCreated, categoriesResolved, attributesCreated, attributesResolved, termsCreated, termsResolved, mediaOutcome.reused, publishedUnexpectedly.get())
        } catch (t: Throwable) {
            TechnicalErrorReporter.report("Product Transfer", "RobustProductTransferService.import", "IMPORT", "ایمپورت محصولات با خطای فنی متوقف شد.", t, "PRODUCT_TRANSFER_IMPORT_EXCEPTION", "storeId=${storeId.value}; source=$source; mode=$mode")
            RobustProductTransferResult(failed = 1, errors = listOf(t.message ?: "خواندن فایل ناموفق بود."), importErrors = listOf(t.message ?: "خواندن فایل ناموفق بود."))
        }
    }

    private suspend fun resolveGlobalAttributes(storeId: StoreId, source: List<TransferGlobalAttribute>, preserveIds: Boolean): GlobalMapping {
        val destination = reader.attributes(storeId)
        val items = mutableMapOf<String, EntityId>()
        var created = 0; var resolved = 0; var termsCreated = 0; var termsResolved = 0
        for (g in source) {
            val existing = if (preserveIds) destination.firstOrNull { it.id.value == g.id } else destination.firstOrNull { normalize(it.slug) == normalize(g.slug) || normalize(it.name) == normalize(g.name) }
            val attr = existing ?: when (val r = d.createAttribute(storeId, GlobalAttribute(EntityId(NEW_ID_PLACEHOLDER), g.name, g.slug, emptyList()))) {
                is CoreResult.Success -> { created++; r.value }
                is CoreResult.Failure -> { TechnicalErrorReporter.reportHandled("Product Transfer", "RobustProductTransferService.attributes", "CREATE_ATTRIBUTE", "ساخت ویژگی سراسری ناموفق بود.", r.error.toString(), "PRODUCT_TRANSFER_ATTRIBUTE_ERROR", "attribute=${g.name}; sourceId=${g.id}"); continue }
            }
            if (existing != null) resolved++
            items[g.id] = attr.id
            val destinationTerms = reader.terms(storeId, attr.id)
            for (term in g.terms) {
                val found = destinationTerms.firstOrNull { normalize(it.name) == normalize(term.name) || normalize(it.slug) == normalize(term.slug) }
                if (found != null) termsResolved++ else when (val r = d.createTerm(storeId, attr.id, AttributeTerm(EntityId(NEW_ID_PLACEHOLDER), term.name, term.slug))) {
                    is CoreResult.Success -> termsCreated++
                    is CoreResult.Failure -> TechnicalErrorReporter.reportHandled("Product Transfer", "RobustProductTransferService.terms", "CREATE_TERM", "ساخت مقدار ویژگی ناموفق بود.", r.error.toString(), "PRODUCT_TRANSFER_TERM_ERROR", "attribute=${g.name}; term=${term.name}")
                }
            }
        }
        return GlobalMapping(items, created, resolved, termsCreated, termsResolved)
    }

    private suspend fun resolveCategories(storeId: StoreId, source: List<TransferCategory>, destination: List<IdName>, createMissing: Boolean): CategoryMapping {
        val sourceById = source.associateBy { it.id }
        val resolved = mutableMapOf<String, IdName>()
        var created = 0; var reused = 0
        val destinationByKey = destination.groupBy { normalize(it.name) + "|" + normalize(it.parentId?.value) }.toMutableMap()
        suspend fun resolve(id: String): IdName? {
            resolved[id]?.let { return it }
            val c = sourceById[id] ?: return null
            val parent = c.parentId?.let { resolve(it) }
            val key = normalize(c.name) + "|" + normalize(parent?.id?.value)
            val found = destinationByKey[key].orEmpty().singleOrNull()
            if (found != null) { resolved[id] = found; reused++; return found }
            if (!createMissing) return null
            return when (val r = d.getProductCategories.create(storeId, IdName(EntityId(NEW_ID_PLACEHOLDER), c.name, parent?.id))) {
                is CoreResult.Success -> { resolved[id] = r.value; destinationByKey[key] = destinationByKey[key].orEmpty() + r.value; created++; r.value }
                is CoreResult.Failure -> { TechnicalErrorReporter.reportHandled("Product Transfer", "RobustProductTransferService.categories", "CREATE_CATEGORY", "ساخت دسته‌بندی ناموفق بود.", r.error.toString(), "PRODUCT_TRANSFER_CATEGORY_ERROR", "category=${c.name}; sourceId=${c.id}; parentId=${c.parentId}"); null }
            }
        }
        source.forEach { resolve(it.id) }
        return CategoryMapping(resolved, created, reused)
    }

    private fun expandCategoryChain(categories: List<IdName>, all: List<IdName>): List<IdName> {
        val byId = all.associateBy { it.id.value }
        val out = linkedMapOf<String, IdName>()
        fun add(c: IdName) { if (out.putIfAbsent(c.id.value, c) == null) c.parentId?.let { byId[it.value]?.let(::add) } }
        categories.forEach(::add)
        return out.values.toList()
    }

    private fun findProductMatch(x: TransferProduct, sameStore: Boolean, byId: Map<String, Product>, bySku: Map<String, Product>, byFingerprint: Map<String, List<Product>>): Product? {
        if (sameStore) byId[x.id]?.let { return it }
        cleanSku(x.sku)?.let { bySku[normalize(it)]?.let { return it } }
        return byFingerprint[x.matchKey()].orEmpty().singleOrNull()
    }

    private fun findVariationByContent(existing: List<Variation>, source: TransferVariation): Variation? {
        cleanSku(source.sku)?.let { s -> existing.firstOrNull { cleanSku(it.sku)?.let(::normalize) == normalize(s) } }?.let { return it }
        val key = source.attributes.map { normalize(it.name) + "=" + normalize(it.option) }.sorted().joinToString("|")
        return existing.singleOrNull { it.attributeKey() == key }
    }

    private suspend fun requireStore(id: StoreId) = when (val result = d.getStore(id)) {
        is CoreResult.Success -> result.value
        is CoreResult.Failure -> error("فروشگاه در دسترس نیست.")
    }
}

private data class CategoryMapping(val items: Map<String, IdName>, val created: Int, val resolved: Int)
private data class GlobalMapping(val items: Map<String, EntityId>, val created: Int, val resolved: Int, val termsCreated: Int, val termsResolved: Int)
