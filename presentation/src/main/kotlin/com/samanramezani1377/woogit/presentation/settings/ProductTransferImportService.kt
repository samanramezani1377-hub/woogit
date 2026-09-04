package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ProductType
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

private const val PAGE_SIZE = 100
private const val IMPORT_CONCURRENCY = 5
private val transferJson = kotlinx.serialization.json.Json { prettyPrint = false; ignoreUnknownKeys = true; explicitNulls = false }

class ProductTransferImportService(private val d: V1PresentationDependencies, private val resolver: ContentResolver) {
    private val reader = ProductTransferRepositoryReader(d, PAGE_SIZE)
    private val media = ProductTransferMedia(d, resolver)
    private val categoryResolver = ProductImportCategoryResolver(d)
    private val attributeResolver = ProductImportAttributeResolver(d, reader)

    suspend fun import(storeId: StoreId, source: Uri, mode: ProductImportMode = ProductImportMode.UPDATE_EXISTING, options: ProductImportOptions = ProductImportOptions(), onProgress: (ProductTransferProgress) -> Unit = {}): RobustProductTransferResult = withContext(Dispatchers.IO) {
        try {
            requireStore(storeId)
            val validated = validateTransferPackage(resolver, source, transferJson)
            if (validated.validationErrors.isNotEmpty()) return@withContext RobustProductTransferResult(failed = validated.invalidProductIds.size, errors = validated.validationErrors, validationErrors = validated.validationErrors)
            val store = requireStore(storeId)
            val sameStore = validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'), true)
            val mediaOutcome = try { media.upload(storeId, source, validated.products, onProgress, options.uploadAllImagesWithoutLibraryCheck) } catch (t: Throwable) { TransferMediaOutcome(emptyMap(), 0, listOf(t.message ?: "خطای پردازش تصاویر"), 0) }
            val existing = reader.products(storeId, onProgress)
            val byId = existing.associateBy { it.id.value }
            val bySku = existing.groupBy { cleanSku(it.sku)?.let(::normalize) }.mapNotNull { (k,v) -> k?.let { key -> v.singleOrNull()?.let { key to it } } }.toMap()
            val byFingerprint = existing.groupBy { it.matchKey() }
            val usedSku = existing.mapNotNullTo(mutableSetOf()) { cleanSku(it.sku)?.let(::normalize) }
            existing.filter { it.type == ProductType.VARIABLE }.forEach { product -> reader.variations(storeId, product.id).forEach { v -> cleanSku(v.sku)?.let { usedSku += normalize(it) } } }
            val destinationCategories = reader.categories(storeId)
            val sourceCategories = validated.products.flatMap { it.categories }.distinctBy { it.id }
            val categoryMap = categoryResolver.resolve(storeId, sourceCategories, destinationCategories, options.addMissingCategories)
            val globalMap = attributeResolver.resolve(storeId, validated.globalAttributes, options.addMissingAttributes, mode == ProductImportMode.UPDATE_EXISTING && sameStore)
            val created = AtomicInteger(0); val updated = AtomicInteger(0); val drafted = AtomicInteger(0); val failed = AtomicInteger(0)
            val variationsCreated = AtomicInteger(0); val variationsUpdated = AtomicInteger(0); val variationsFailed = AtomicInteger(0); val skuChanged = AtomicInteger(0); val publishedUnexpectedly = AtomicInteger(0)
            val usedMedia = ConcurrentHashMap.newKeySet<String>(); val errors = ConcurrentLinkedQueue<String>(); val importErrors = ConcurrentLinkedQueue<String>(); val skuMutex = Mutex()
            coroutineScope { validated.products.mapIndexed { index, x -> async(Dispatchers.IO.limitedParallelism(IMPORT_CONCURRENCY)) {
                try {
                    onProgress(ProductTransferProgress("در حال وارد کردن محصولات…", index + 1, validated.products.size)); var productCountedAsSuccess = false; var reservedSku: String? = null
                    try {
                        val old = if (mode == ProductImportMode.UPDATE_EXISTING) ProductImportMatching.findProduct(x, sameStore, byId, bySku, byFingerprint) else null
                        val images = x.images.mapNotNull { image -> mediaOutcome.images[image.file]?.also { usedMedia += image.file } }
                        val categories = x.categories.mapNotNull { category -> categoryMap.items[category.id] }
                        if (categories.size < x.categories.size) errors += "${x.name}: برخی دسته‌بندی‌ها در مقصد Resolve نشدند."
                        val attributes = x.attributes.map { a -> com.samanramezani1377.woogit.core.domain.model.Attribute(a.id?.let { globalMap.items[it] ?: if (mode == ProductImportMode.UPDATE_EXISTING && sameStore) EntityId(it) else null }, a.name, a.visible, a.variation, a.options) }
                        reservedSku = if (old == null) skuMutex.withLock { reserveNewSku(x.sku, usedSku) { skuChanged.incrementAndGet() } } else null
                        var product = x.toDomain(if (old == null) EntityId(NEW_ID_PLACEHOLDER) else old.id, images, categories, attributes).copy(sku = if (old == null) reservedSku else x.sku)
                        if (mode == ProductImportMode.CREATE_NEW_DRAFT) product = product.copy(status = com.samanramezani1377.woogit.core.domain.model.ProductStatus.DRAFT)
                        val saved = if (old == null) d.createProduct(storeId, product) else d.updateProduct(storeId, old.id, product)
                        val savedProduct = when (saved) {
                            is CoreResult.Success -> { if (old == null) { created.incrementAndGet(); if (mode == ProductImportMode.CREATE_NEW_DRAFT) drafted.incrementAndGet() } else updated.incrementAndGet(); productCountedAsSuccess = true; saved.value }
                            is CoreResult.Failure -> { importErrors += "${x.name}: ${saved.error}"; null }
                        }
                        if (savedProduct == null) return@async
                        var finalProduct = savedProduct
                        if (old == null && mode == ProductImportMode.CREATE_NEW_DRAFT && savedProduct.status != com.samanramezani1377.woogit.core.domain.model.ProductStatus.DRAFT) {
                            val pendingProduct = product.copy(id = savedProduct.id, status = com.samanramezani1377.woogit.core.domain.model.ProductStatus.PENDING)
                            when (val pendingResult = d.updateProduct(storeId, savedProduct.id, pendingProduct)) {
                                is CoreResult.Success -> { finalProduct = pendingResult.value; if (finalProduct.status == com.samanramezani1377.woogit.core.domain.model.ProductStatus.PUBLISHED) { publishedUnexpectedly.incrementAndGet(); importErrors.add("${x.name}: پیش‌نویس و Pending اعمال نشد؛ محصول منتشر شد.") } }
                                is CoreResult.Failure -> { val observed = runCatching { d.getProduct(storeId, savedProduct.id) }.getOrNull(); val observedProduct = (observed as? CoreResult.Success)?.value; finalProduct = observedProduct ?: savedProduct; if (observedProduct?.status == com.samanramezani1377.woOGit.core.domain.model.ProductStatus.PUBLISHED || finalProduct.status == com.samanramezani1377.woogit.core.domain.model.ProductStatus.PUBLISHED) { publishedUnexpectedly.incrementAndGet(); importErrors.add("${x.name}: پیش‌نویس و Pending اعمال نشد؛ محصول منتشر شد.") } }
                            }
                        }
                        val existingVariations = try { reader.variations(storeId, finalProduct.id) } catch (t: Throwable) { variationsFailed.addAndGet(x.variations.size); emptyList() }
                        x.variations.forEach { sourceVariation ->
                            var reservedVariationSku: String? = null; var variationCommitted = false
                            try {
                                val oldVariation = if (old == null) null else if (sameStore) existingVariations.firstOrNull { it.id.value == sourceVariation.id } ?: ProductImportMatching.findVariation(existingVariations, sourceVariation) else ProductImportMatching.findVariation(existingVariations, sourceVariation)
                                val image = sourceVariation.image?.let { mediaOutcome.images[it.file]?.also { usedMedia += sourceVariation.image.file } }
                                reservedVariationSku = if (oldVariation == null) skuMutex.withLock { reserveNewSku(sourceVariation.sku, usedSku) { skuChanged.incrementAndGet() } } else null
                                val variation = sourceVariation.toDomain(finalProduct.id, oldVariation?.id ?: EntityId(NEW_ID_PLACEHOLDER), image).copy(sku = if (oldVariation == null) reservedVariationSku else sourceVariation.sku)
                                when (val result = if (oldVariation == null) d.createVariation(storeId, variation) else d.updateVariation(storeId, finalProduct.id, oldVariation.id, variation)) {
                                    is CoreResult.Success -> { if (oldVariation == null) variationsCreated.incrementAndGet() else variationsUpdated.incrementAndGet(); variationCommitted = true }
                                    is CoreResult.Failure -> { variationsFailed.incrementAndGet(); importErrors += "${x.name}: variation ${sourceVariation.sku ?: sourceVariation.id} وارد نشد: ${result.error}" }
                                }
                            } catch (t: Throwable) { variationsFailed.incrementAndGet(); importErrors += "${x.name}: variation ${sourceVariation.sku ?: sourceVariation.id} با خطای فنی مواجه شد: ${t.message ?: "خطای نامشخص"}" } finally { if (!variationCommitted) releaseSku(reservedVariationSku, usedSku) }
                        }
                    } catch (t: Throwable) { if (!productCountedAsSuccess) failed.incrementAndGet(); importErrors += "${x.name}: ${t.message ?: "خطای نامشخص"}" } finally { if (!productCountedAsSuccess) skuMutex.withLock { releaseSku(reservedSku, usedSku) } }
                } catch (t: Throwable) { failed.incrementAndGet(); importErrors += "${x.name}: ${t.message ?: "خطای نامشخص"}" }
            } }.awaitAll() }
            RobustProductTransferResult(created.get(), updated.get(), failed.get(), mediaOutcome.uploaded, variationsCreated.get(), variationsUpdated.get(), (mediaOutcome.errors + errors + importErrors).distinct().take(50), variationsFailed.get(), mediaOutcome.failed, (mediaOutcome.images.keys - usedMedia).size, skuChanged.get(), emptyList(), importErrors.distinct().take(50), drafted.get(), categoryMap.created, categoryMap.resolved, globalMap.created, globalMap.resolved, globalMap.termsCreated, globalMap.termsResolved, mediaOutcome.reused, publishedUnexpectedly.get())
        } catch (t: Throwable) { RobustProductTransferResult(failed = 1, errors = listOf(t.message ?: "خواندن فایل ناموفق بود."), importErrors = listOf(t.message ?: "خواندن فایل ناموفق بود.")) }
    }

    private suspend fun requireStore(id: StoreId) = when (val result = d.getStore(id)) { is CoreResult.Success -> result.value; is CoreResult.Failure -> error("فروشگاه در دسترس نیست.") }
}
