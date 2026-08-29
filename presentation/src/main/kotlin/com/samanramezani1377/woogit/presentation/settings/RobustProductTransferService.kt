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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream

private const val FORMAT="woogit-products"
private const val VERSION=1
private const val PAGE_SIZE=100
private const val MAX_PRODUCTS=10_000
private const val MAX_VARIATIONS_PER_PRODUCT=10_000
private const val MAX_PACKAGE_BYTES=1_073_741_824L
private const val MAX_ENTRY_BYTES=50L*1024L*1024L
private const val NEW_ID_PLACEHOLDER="new"
private val transferJson=Json{prettyPrint=false;ignoreUnknownKeys=true;explicitNulls=false}

class RobustProductTransferService(private val d:V1PresentationDependencies,private val resolver:ContentResolver){
    private val reader=ProductTransferRepositoryReader(d,PAGE_SIZE)
    private val media=ProductTransferMedia(d,resolver)

    suspend fun export(storeId:StoreId,destination:Uri,onProgress:(ProductTransferProgress)->Unit={}):Result<Int>=withContext(Dispatchers.IO){runCatching{
        val store=requireStore(storeId);val products=reader.products(storeId,onProgress);require(products.size<=MAX_PRODUCTS){"تعداد محصولات از حد مجاز بیشتر است."};var imageCount=0
        resolver.openOutputStream(destination)?.use{raw->CountingOutputStream(raw,MAX_PACKAGE_BYTES).use{counted->ZipOutputStream(counted).use{zip->
            val exported=products.mapIndexed{index,product->
                onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…",index+1,products.size))
                val images=product.images.mapIndexed{imageIndex,image->val file="media/p-${product.id.value}-$imageIndex.${transferExt(image.src)}";val bytes=downloadTransferImage(image.src)?:error("تصویر «${image.name?:image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.");require(bytes.size.toLong()<=MAX_ENTRY_BYTES);writeTransferEntry(zip,file,bytes);imageCount++;TransferImage(image.id?.value,image.src,image.name,image.alt,file)}
                val variations=if(product.type==ProductType.VARIABLE){val all=reader.variations(storeId,product.id);require(all.size<=MAX_VARIATIONS_PER_PRODUCT){"تعداد Variationهای محصول بیش از حد مجاز است."};all.map{variation->variation.toTransfer{image->val file="media/v-${variation.id.value}.${transferExt(image.src)}";val bytes=downloadTransferImage(image.src)?:error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد.");require(bytes.size.toLong()<=MAX_ENTRY_BYTES);writeTransferEntry(zip,file,bytes);imageCount++;file}}}else emptyList()
                product.toTransfer(images,variations)
            }
            val manifest=ProductTransferManifest(FORMAT,VERSION,store.baseUrl.trimEnd('/'),SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(Date()),exported.size,imageCount)
            writeTransferEntry(zip,"manifest.json",transferJson.encodeToString(manifest).toByteArray());writeTransferEntry(zip,"products.json",transferJson.encodeToString(ProductTransferPackage(manifest,exported)).toByteArray())
        }}}?:error("امکان ایجاد فایل خروجی وجود ندارد.")
        products.size
    }}

    suspend fun import(storeId:StoreId,source:Uri,onProgress:(ProductTransferProgress)->Unit={}):RobustProductTransferResult=withContext(Dispatchers.IO){try{
        val store=requireStore(storeId)
        val validated=validateTransferPackage(resolver,source,transferJson)
        if(validated.validationErrors.isNotEmpty())return@withContext RobustProductTransferResult(failed=validated.invalidProductIds.size,errors=validated.validationErrors,validationErrors=validated.validationErrors)
        val sameStore=validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'),true)
        val mediaOutcome=media.upload(storeId,source,validated.products,onProgress)
        val existing=reader.products(storeId,onProgress)
        val byId=existing.associateBy{it.id.value}
        val bySku=existing.mapNotNull{p->cleanSku(p.sku)?.let{normalize(it) to p}}.toMap()
        val byFingerprint=existing.groupBy{it.matchKey()}
        val usedSku=existing.mapNotNullTo(mutableSetOf()){cleanSku(it.sku)?.let(::normalize)}
        existing.filter{it.type==ProductType.VARIABLE}.forEach{product->reader.variations(storeId,product.id).forEach{variation->cleanSku(variation.sku)?.let{usedSku+=normalize(it)}}}
        val destinationCategories=reader.categories(storeId)
        val sourceCategories=validated.products.flatMap{it.categories}.distinctBy{it.id}
        val categoryMap=resolveCategories(sourceCategories,destinationCategories,sameStore)
        var created=0;var updated=0;var failed=0;var variationsCreated=0;var variationsUpdated=0;var variationsFailed=0;var skuChanged=0
        val usedMedia=mutableSetOf<String>();val errors=mutableListOf<String>();val importErrors=mutableListOf<String>()
        validated.products.forEachIndexed{index,x->
            onProgress(ProductTransferProgress("در حال وارد کردن محصولات…",index+1,validated.products.size))
            try{
                val old=findProductMatch(x,sameStore,byId,bySku,byFingerprint)
                val images=x.images.mapNotNull{image->mediaOutcome.images[image.file]?.also{usedMedia+=image.file}}
                val categories=x.categories.mapNotNull{categoryMap[it.id]}
                if(categories.size<x.categories.size)errors+="${x.name}: برخی دسته‌بندی‌ها در مقصد Resolve نشدند."
                val attributes=x.attributes.map{attribute->Attribute(if(sameStore)attribute.id?.let(::EntityId)else null,attribute.name,attribute.visible,attribute.variation,attribute.options)}
                val reservedSku=if(old==null)reserveNewSku(x.sku,usedSku){skuChanged++}else null
                val product=if(old==null)x.toDomain(EntityId(NEW_ID_PLACEHOLDER),images,categories,attributes).copy(sku=reservedSku)else x.toDomain(old.id,images,categories,attributes).copy(id=old.id)
                val saved=if(old==null)d.createProduct(storeId,product)else d.updateProduct(storeId,old.id,product)
                val savedProduct=when(saved){is CoreResult.Success->{if(old==null)created++else updated++;saved.value};is CoreResult.Failure->{failed++;importErrors+="${x.name}: ${saved.error}";releaseSku(reservedSku,usedSku);return@forEachIndexed}}
                val existingVariations=reader.variations(storeId,savedProduct.id)
                x.variations.forEach{sourceVariation->
                    val oldVariation=when{sameStore->existingVariations.firstOrNull{it.id.value==sourceVariation.id};else->cleanSku(sourceVariation.sku)?.let{sku->existingVariations.firstOrNull{cleanSku(it.sku)?.let(::normalize)==normalize(sku)}}?:existingVariations.firstOrNull{it.attributeKey()==sourceVariation.attributeKey()}}
                    val image=sourceVariation.image?.let{mediaOutcome.images[it.file]?.also{usedMedia+=sourceVariation.image.file}}
                    val reservedVariationSku=if(oldVariation==null)reserveNewSku(sourceVariation.sku,usedSku){skuChanged++}else null
                    val variation=if(oldVariation==null)sourceVariation.toDomain(savedProduct.id,EntityId(NEW_ID_PLACEHOLDER),image).copy(sku=reservedVariationSku)else sourceVariation.toDomain(savedProduct.id,oldVariation.id,image).copy(id=oldVariation.id)
                    when(val result=if(oldVariation==null)d.createVariation(storeId,variation)else d.updateVariation(storeId,savedProduct.id,oldVariation.id,variation)){
                        is CoreResult.Success->{if(oldVariation==null)variationsCreated++else variationsUpdated++}
                        is CoreResult.Failure->{variationsFailed++;importErrors+="${x.name}: variation ${sourceVariation.sku?:sourceVariation.id} وارد نشد: ${result.error}";releaseSku(reservedVariationSku,usedSku)}
                    }
                }
            }catch(t:Throwable){failed++;importErrors+="${x.name}: ${t.message?:"خطای نامشخص"}"}
        }
        RobustProductTransferResult(created,updated,failed,mediaOutcome.uploaded,variationsCreated,variationsUpdated,(mediaOutcome.errors+errors+importErrors).distinct().take(50),variationsFailed,mediaOutcome.failed,(mediaOutcome.images.keys-usedMedia).size,skuChanged,emptyList(),importErrors.distinct().take(50))
    }catch(t:Throwable){RobustProductTransferResult(failed=1,errors=listOf(t.message?:"خواندن فایل ناموفق بود."))}}

    private fun findProductMatch(x:TransferProduct,sameStore:Boolean,byId:Map<String,Product>,bySku:Map<String,Product>,byFingerprint:Map<String,List<Product>>):Product?{if(sameStore)byId[x.id]?.let{return it};cleanSku(x.sku)?.let{bySku[normalize(it)]?.let{return it}};return byFingerprint[x.matchKey()].orEmpty().singleOrNull()}

    private fun resolveCategories(source:List<TransferCategory>,destination:List<IdName>,sameStore:Boolean):Map<String,IdName>{val sourceById=source.associateBy{it.id};val destinationByKey=destination.groupBy{normalize(it.name)+"|"+normalize(it.parentId?.value)};val resolved=mutableMapOf<String,IdName>();fun resolve(id:String):IdName?{resolved[id]?.let{return it};val sourceCategory=sourceById[id]?:return null;if(sameStore){destination.firstOrNull{it.id.value==id}?.let{resolved[id]=it;return it}};val parent=sourceCategory.parentId?.let(::resolve);val candidate=destinationByKey[normalize(sourceCategory.name)+"|"+normalize(parent?.id?.value)].orEmpty().singleOrNull();if(candidate!=null)resolved[id]=candidate;return candidate};source.forEach{resolve(it.id)};return resolved}

    private fun requireStore(id:StoreId)=when(val result=d.getStore(id)){is CoreResult.Success->result.value;is CoreResult.Failure->error("فروشگاه در دسترس نیست.")}
}
