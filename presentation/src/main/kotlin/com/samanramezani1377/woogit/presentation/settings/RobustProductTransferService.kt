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
        val store=requireStore(storeId)
        val products=reader.products(storeId,onProgress)
        require(products.size<=MAX_PRODUCTS){"تعداد محصولات از حد مجاز بیشتر است."}
        val usedGlobalIds=products.flatMap{it.attributes}.mapNotNull{it.id?.value}.toSet()
        val globals=reader.attributes(storeId).filter{it.id.value in usedGlobalIds}.map{g->TransferGlobalAttribute(g.id.value,g.name,g.slug,reader.terms(storeId,g.id).map{TransferTerm(it.id.value,it.name,it.slug)})}
        var imageCount=0
        resolver.openOutputStream(destination)?.use{raw->CountingOutputStream(raw,MAX_PACKAGE_BYTES).use{counted->ZipOutputStream(counted).use{zip->
            val exported=products.mapIndexed{index,product->
                onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…",index+1,products.size))
                val images=product.images.mapIndexed{imageIndex,image->{val file="media/p-${product.id.value}-$imageIndex.${transferExt(image.src)}";val bytes=downloadTransferImage(image.src)?:error("تصویر «${image.name?:image.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.");require(bytes.size.toLong()<=MAX_ENTRY_BYTES);writeTransferEntry(zip,file,bytes);imageCount++;TransferImage(image.id?.value,image.src,image.name,image.alt,file)}}
                val variations=if(product.type==ProductType.VARIABLE){val all=reader.variations(storeId,product.id);require(all.size<=MAX_VARIATIONS_PER_PRODUCT){"تعداد Variationهای محصول بیش از حد مجاز است."};all.map{variation->variation.toTransfer{image->{val file="media/v-${variation.id.value}.${transferExt(image.src)}";val bytes=downloadTransferImage(image.src)?:error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد.");require(bytes.size.toLong()<=MAX_ENTRY_BYTES);writeTransferEntry(zip,file,bytes);imageCount++;file}}}}else emptyList()
                product.toTransfer(images,variations)
            }
            val manifest=ProductTransferManifest(FORMAT,VERSION,store.baseUrl.trimEnd('/'),SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(Date()),exported.size,imageCount)
            writeTransferEntry(zip,"manifest.json",transferJson.encodeToString(manifest).toByteArray())
            writeTransferEntry(zip,"products.json",transferJson.encodeToString(ProductTransferPackage(manifest,exported,globals)).toByteArray())
        }}}?:error("امکان ایجاد فایل خروجی وجود ندارد.")
        products.size
    }}

    suspend fun import(storeId:StoreId,source:Uri,mode:ProductImportMode=ProductImportMode.UPDATE_EXISTING,onProgress:(ProductTransferProgress)->Unit={}):RobustProductTransferResult=withContext(Dispatchers.IO){try{
        val store=requireStore(storeId)
        val validated=validateTransferPackage(resolver,source,transferJson)
        if(validated.validationErrors.isNotEmpty())return@withContext RobustProductTransferResult(failed=validated.invalidProductIds.size,errors=validated.validationErrors,validationErrors=validated.validationErrors)
        val sameStore=validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'),true)
        val mediaOutcome=media.upload(storeId,source,validated.products,onProgress)
        val existing=reader.products(storeId,onProgress)
        val byId=existing.associateBy{it.id.value}
        val bySku=existing.groupBy{cleanSku(it.sku)?.let(::normalize)}.mapNotNull{(k,v)->k?.let{it to v.singleOrNull()}}.toMap()
        val byFingerprint=existing.groupBy{it.matchKey()}
        val usedSku=existing.mapNotNullTo(mutableSetOf()){cleanSku(it.sku)?.let(::normalize)}
        existing.filter{it.type==ProductType.VARIABLE}.forEach{product->reader.variations(storeId,product.id).forEach{variation->cleanSku(variation.sku)?.let{usedSku+=normalize(it)}}}
        val destinationCategories=reader.categories(storeId)
        val sourceCategories=validated.products.flatMap{it.categories}.distinctBy{it.id}
        val categoryMap=resolveCategories(storeId,sourceCategories,destinationCategories,mode==ProductImportMode.UPDATE_EXISTING&&sameStore)
        val globalMap=resolveGlobalAttributes(storeId,validated.globalAttributes,mode==ProductImportMode.UPDATE_EXISTING&&sameStore)
        var created=0;var updated=0;var drafted=0;var failed=0;var variationsCreated=0;var variationsUpdated=0;var variationsFailed=0;var skuChanged=0;var categoriesCreated=0;var categoriesResolved=0;var attributesCreated=0;var attributesResolved=0;var termsCreated=0;var termsResolved=0
        val usedMedia=mutableSetOf<String>();val errors=mutableListOf<String>();val importErrors=mutableListOf<String>()
        categoriesCreated=categoryMap.created;categoriesResolved=categoryMap.resolved
        attributesCreated=globalMap.created;attributesResolved=globalMap.resolved;termsCreated=globalMap.termsCreated;termsResolved=globalMap.termsResolved
        validated.products.forEachIndexed{index,x->
            onProgress(ProductTransferProgress("در حال وارد کردن محصولات…",index+1,validated.products.size))
            try{
                val old=if(mode==ProductImportMode.UPDATE_EXISTING)findProductMatch(x,sameStore,byId,bySku,byFingerprint) else null
                val images=x.images.mapNotNull{image->mediaOutcome.images[image.file]?.also{usedMedia+=image.file}}
                val categories=x.categories.mapNotNull{categoryMap.items[it.id]}
                if(categories.size<x.categories.size)errors+="${x.name}: برخی دسته‌بندی‌ها در مقصد Resolve نشدند."
                val attributes=x.attributes.map{a->Attribute(a.id?.let{globalMap.items[it]?:if(mode==ProductImportMode.UPDATE_EXISTING&&sameStore)EntityId(it)else null},a.name,a.visible,a.variation,a.options)}
                val reservedSku=if(old==null)reserveNewSku(x.sku,usedSku){skuChanged++}else null
                var product=x.toDomain(if(old==null)EntityId(NEW_ID_PLACEHOLDER)else old.id,images,categories,attributes).copy(sku=if(old==null)reservedSku else x.sku)
                if(mode==ProductImportMode.CREATE_NEW_DRAFT)product=product.copy(status=ProductStatus.DRAFT)
                val saved=if(old==null)d.createProduct(storeId,product)else d.updateProduct(storeId,old.id,product)
                val savedProduct=when(saved){is CoreResult.Success->{if(old==null){created++;if(mode==ProductImportMode.CREATE_NEW_DRAFT)drafted++}else updated++;saved.value};is CoreResult.Failure->{failed++;importErrors+="${x.name}: ${saved.error}";releaseSku(reservedSku,usedSku);return@forEachIndexed}}
                val existingVariations=reader.variations(storeId,savedProduct.id)
                x.variations.forEach{sourceVariation->
                    val oldVariation=if(old==null)null else if(sameStore)existingVariations.firstOrNull{it.id.value==sourceVariation.id}?:findVariationByContent(existingVariations,sourceVariation) else findVariationByContent(existingVariations,sourceVariation)
                    val image=sourceVariation.image?.let{mediaOutcome.images[it.file]?.also{usedMedia+=sourceVariation.image.file}}
                    val reservedVariationSku=if(oldVariation==null)reserveNewSku(sourceVariation.sku,usedSku){skuChanged++}else null
                    val variation=sourceVariation.toDomain(savedProduct.id,if(oldVariation==null)EntityId(NEW_ID_PLACEHOLDER)else oldVariation.id,image).copy(sku=if(oldVariation==null)reservedVariationSku else sourceVariation.sku)
                    when(val result=if(oldVariation==null)d.createVariation(storeId,variation)else d.updateVariation(storeId,savedProduct.id,oldVariation.id,variation)){
                        is CoreResult.Success->{if(oldVariation==null)variationsCreated++else variationsUpdated++}
                        is CoreResult.Failure->{variationsFailed++;importErrors+="${x.name}: variation ${sourceVariation.sku?:sourceVariation.id} وارد نشد: ${result.error}";releaseSku(reservedVariationSku,usedSku)}
                    }
                }
            }catch(t:Throwable){failed++;importErrors+="${x.name}: ${t.message?:"خطای نامشخص"}"}
        }
        RobustProductTransferResult(created,updated,failed,mediaOutcome.uploaded,variationsCreated,variationsUpdated,(mediaOutcome.errors+errors+importErrors).distinct().take(50),variationsFailed,mediaOutcome.failed,(mediaOutcome.images.keys-usedMedia).size,skuChanged,emptyList(),importErrors.distinct().take(50),drafted,categoriesCreated,categoriesResolved,attributesCreated,attributesResolved,termsCreated,termsResolved)
    }catch(t:Throwable){RobustProductTransferResult(failed=1,errors=listOf(t.message?:"خواندن فایل ناموفق بود."))}}

    private suspend fun resolveGlobalAttributes(storeId:StoreId,source:List<TransferGlobalAttribute>,preserveIds:Boolean):GlobalMapping{val destination=reader.attributes(storeId);val items=mutableMapOf<String,EntityId>();var created=0;var resolved=0;var termsCreated=0;var termsResolved=0;for(g in source){val existing=if(preserveIds)destination.firstOrNull{it.id.value==g.id} else destination.firstOrNull{normalize(it.slug)==normalize(g.slug)||normalize(it.name)==normalize(g.name)};val attr=existing?:when(val r=d.createAttribute(storeId,GlobalAttribute(EntityId(NEW_ID_PLACEHOLDER),g.name,g.slug,emptyList()))){is CoreResult.Success->{created++;r.value};is CoreResult.Failure->{continue}};if(existing!=null)resolved++;items[g.id]=attr.id;val destinationTerms=reader.terms(storeId,attr.id);for(term in g.terms){val found=destinationTerms.firstOrNull{normalize(it.name)==normalize(term.name)||normalize(it.slug)==normalize(term.slug)};if(found!=null)termsResolved++ else when(val r=d.createTerm(storeId,attr.id,AttributeTerm(EntityId(NEW_ID_PLACEHOLDER),term.name,term.slug))){is CoreResult.Success->termsCreated++;is CoreResult.Failure->Unit}}};return GlobalMapping(items,created,resolved,termsCreated,termsResolved)}

    private suspend fun resolveCategories(storeId:StoreId,source:List<TransferCategory>,destination:List<IdName>,createMissing:Boolean):CategoryMapping{val sourceById=source.associateBy{it.id};val resolved=mutableMapOf<String,IdName>();var created=0;var reused=0;fun key(name:String,parent:EntityId?)=normalize(name)+"|"+normalize(parent?.value);val destinationByKey=destination.groupBy{key(it.name,it.parentId)}
        suspend fun resolve(id:String):IdName?{resolved[id]?.let{return it};val c=sourceById[id]?:return null;val parent=c.parentId?.let{resolve(it)};val found=destinationByKey[key(c.name,parent?.id)].orEmpty().singleOrNull();if(found!=null){resolved[id]=found;reused++;return found};if(!createMissing)return null;return when(val r=d.getProductCategories.create(storeId,IdName(EntityId(NEW_ID_PLACEHOLDER),c.name,parent?.id))){is CoreResult.Success->{resolved[id]=r.value;created++;r.value};is CoreResult.Failure->null}}
        source.forEach{resolve(it.id)};return CategoryMapping(resolved,created,reused)}

    private fun findProductMatch(x:TransferProduct,sameStore:Boolean,byId:Map<String,Product>,bySku:Map<String,Product>,byFingerprint:Map<String,List<Product>>):Product?{if(sameStore)byId[x.id]?.let{return it};cleanSku(x.sku)?.let{bySku[normalize(it)]?.let{return it}};return byFingerprint[x.matchKey()].orEmpty().singleOrNull()}
    private fun findVariationByContent(existing:List<Variation>,source:TransferVariation):Variation?{cleanSku(source.sku)?.let{s->existing.firstOrNull{cleanSku(it.sku)?.let(::normalize)==normalize(s)}}?.let{return it};val key=source.attributes.map{normalize(it.name)+"="+normalize(it.option)}.sorted().joinToString("|");return existing.singleOrNull{it.attributeKey()==key}}
    private fun requireStore(id:StoreId)=when(val result=d.getStore(id)){is CoreResult.Success->result.value;is CoreResult.Failure->error("فروشگاه در دسترس نیست.")}
}

private data class CategoryMapping(val items:Map<String,IdName>,val created:Int,val resolved:Int)
private data class GlobalMapping(val items:Map<String,EntityId>,val created:Int,val resolved:Int,val termsCreated:Int,val termsResolved:Int)
