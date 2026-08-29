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

private const val FORMAT="woogit-products"
private const val VERSION=1
private const val PAGE_SIZE=100
private const val MAX_PRODUCTS=10_000
private const val MAX_VARIATIONS_PER_PRODUCT=10_000
private const val MAX_PACKAGE_BYTES=1_073_741_824L
private const val MAX_ENTRY_BYTES=50L*1024L*1024L
private const val MAX_PRODUCT_JSON_BYTES=100L*1024L*1024L
private const val NEW_ID_PLACEHOLDER="new"
private val transferJson=Json{prettyPrint=false;ignoreUnknownKeys=true;explicitNulls=false}

class RobustProductTransferService(private val d:V1PresentationDependencies,private val resolver:ContentResolver){
    suspend fun export(storeId:StoreId,destination:Uri,onProgress:(ProductTransferProgress)->Unit={}):Result<Int> = withContext(Dispatchers.IO){runCatching{
        val store=requireStore(storeId);val products=allProducts(storeId,onProgress);require(products.size<=MAX_PRODUCTS)
        var imageCount=0
        resolver.openOutputStream(destination)?.use{raw->CountingOutputStream(raw,MAX_PACKAGE_BYTES).use{counted->ZipOutputStream(counted).use{zip->
            val exported=products.mapIndexed{index,p->onProgress(ProductTransferProgress("در حال آماده‌سازی محصولات…",index+1,products.size));val imgs=p.images.mapIndexed{ii,img->val file="media/p-${p.id.value}-$ii.${ext(img.src)}";val bytes=downloadImage(img.src)?:error("تصویر «${img.name?:img.src}» قابل دریافت نیست؛ خروجی ناقص ساخته نشد.");require(bytes.size<=MAX_ENTRY_BYTES);writeZipEntry(zip,file,bytes);imageCount++;TransferImage(img.id?.value,img.src,img.name,img.alt,file)};val vars=if(p.type==ProductType.VARIABLE){val vs=allVars(storeId,p.id);require(vs.size<=MAX_VARIATIONS_PER_PRODUCT);vs.map{v->v.toTransfer{img->val file="media/v-${v.id.value}.${ext(img.src)}";val bytes=downloadImage(img.src)?:error("تصویر Variation قابل دریافت نیست؛ خروجی ناقص ساخته نشد.");require(bytes.size<=MAX_ENTRY_BYTES);writeZipEntry(zip,file,bytes);imageCount++;file}}}else emptyList();p.toTransfer(imgs,vars)}
            val m=ProductTransferManifest(FORMAT,VERSION,store.baseUrl.trimEnd('/'),SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(Date()),exported.size,imageCount);writeZipEntry(zip,"manifest.json",transferJson.encodeToString(m).toByteArray());writeZipEntry(zip,"products.json",transferJson.encodeToString(ProductTransferPackage(m,exported)).toByteArray())
        }}}?:error("امکان ایجاد فایل خروجی وجود ندارد.");products.size
    }}

    suspend fun import(storeId:StoreId,source:Uri,onProgress:(ProductTransferProgress)->Unit={}):RobustProductTransferResult=withContext(Dispatchers.IO){try{
        requireStore(storeId);val validated=validatePackage(source);if(validated.validationErrors.isNotEmpty())return@withContext RobustProductTransferResult(failed=validated.invalidProductIds.size,validationErrors=validated.validationErrors,errors=validated.validationErrors)
        val store=requireStore(storeId);val sameStore=validated.manifest.source.trimEnd('/').equals(store.baseUrl.trimEnd('/'),true);val media=uploadMediaPass(storeId,source,validated.products,onProgress);val existing=allProducts(storeId,onProgress)
        val byId=existing.associateBy{it.id.value};val bySku=existing.mapNotNull{p->cleanSku(p.sku)?.let{normalize(it) to p}}.toMap();val byFingerprint=existing.groupBy{it.matchKey()};val usedSku=existing.mapNotNullTo(mutableSetOf()){cleanSku(it.sku)?.let(::normalize)}
        existing.filter{it.type==ProductType.VARIABLE}.forEach{p->allVars(storeId,p.id).forEach{v->cleanSku(v.sku)?.let{usedSku+=normalize(it)}}}
        val categories=allCategories(storeId);val categoryByName=categories.associateBy{normalize(it.name)}
        var created=0;var updated=0;var failed=0;var vc=0;var vu=0;var vf=0;var skuChanged=0;val usedMedia=mutableSetOf<String>();val errors=mutableListOf<String>();val importErrors=mutableListOf<String>()
        for((index,x) in validated.products.withIndex()){onProgress(ProductTransferProgress("در حال وارد کردن محصولات…",index+1,validated.products.size));try{
            val old=findProductMatch(x,sameStore,byId,bySku,byFingerprint);val images=x.images.mapNotNull{media.images[it.file]?.also{usedMedia+=it.file}};val cats=x.categories.mapNotNull{c->categoryByName[normalize(c.name)]?:if(sameStore)IdName(EntityId(c.id),c.name)else null};val attrs=x.attributes.map{a->Attribute(if(sameStore)a.id?.let(::EntityId)else null,a.name,a.visible,a.variation,a.options)};val reserved=if(old==null)reserveNewSku(x.sku,usedSku){skuChanged++}else null;val product=if(old==null)x.toDomain(EntityId(NEW_ID_PLACEHOLDER),images,cats,attrs).copy(sku=reserved)else x.toDomain(old.id,images,cats,attrs);val saved=if(old==null)d.createProduct(storeId,product)else d.updateProduct(storeId,old.id,product.copy(id=old.id));val savedProduct=when(saved){is CoreResult.Success->{if(old==null)created++else updated++;saved.value};is CoreResult.Failure->{failed++;importErrors+="${x.name}: ${saved.error}";releaseSku(reserved,usedSku);continue}}
            val vars=allVars(storeId,savedProduct.id);for(vv in x.variations){val oldV=if(sameStore)vars.firstOrNull{it.id.value==vv.id}else cleanSku(vv.sku)?.let{s->vars.firstOrNull{cleanSku(it.sku)?.let(::normalize)==normalize(s)}}?:vars.firstOrNull{it.attributeKey()==vv.attributeKey()};val img=vv.image?.let{media.images[it.file]?.also{usedMedia+=vv.image.file}};val reservedV=if(oldV==null)reserveNewSku(vv.sku,usedSku){skuChanged++}else null;val v=if(oldV==null)vv.toDomain(savedProduct.id,EntityId(NEW_ID_PLACEHOLDER),img).copy(sku=reservedV)else vv.toDomain(savedProduct.id,oldV.id,img);when(val r=if(oldV==null)d.createVariation(storeId,v)else d.updateVariation(storeId,savedProduct.id,oldV.id,v.copy(id=oldV.id))){is CoreResult.Success->if(oldV==null)vc++else vu++;is CoreResult.Failure->{vf++;importErrors+="${x.name}: variation ${vv.sku?:vv.id} وارد نشد: ${r.error}";releaseSku(reservedV,usedSku)}}}
        }catch(t:Throwable){failed++;importErrors+="${x.name}: ${t.message?:"خطای نامشخص"}"}}
        RobustProductTransferResult(created,updated,failed,media.uploaded,vc,vu,(media.errors+errors+importErrors).distinct().take(50),vf,media.failed,(media.images.keys-usedMedia).size,skuChanged,emptyList(),importErrors.distinct().take(50))
    }catch(t:Throwable){RobustProductTransferResult(failed=1,errors=listOf(t.message?:"خواندن فایل ناموفق بود."))}}

    private fun validatePackage(uri:Uri):TransferValidatedPackage{var manifest:ProductTransferManifest?=null;var products:List<TransferProduct>?=null;val mediaNames=linkedSetOf<String>();var total=0L;resolver.openInputStream(uri)?.use{input->ZipInputStream(input).use{zip->while(true){val e=zip.nextEntry?:break;if(e.isDirectory)continue;val n=e.name;require(!n.startsWith("/")&&!n.contains("..")){"مسیر نامعتبر داخل فایل WooGit."};when(n){"manifest.json"->{val b=readLimited(zip,MAX_ENTRY_BYTES){total+=it;require(total<=MAX_PACKAGE_BYTES)};manifest=transferJson.decodeFromString(b.toString(Charsets.UTF_8))};"products.json"->{val b=readLimited(zip,MAX_PRODUCT_JSON_BYTES){total+=it;require(total<=MAX_PACKAGE_BYTES)};val p=transferJson.decodeFromString<ProductTransferPackage>(b.toString(Charsets.UTF_8));manifest=manifest?:p.manifest;products=p.products};else->if(n.startsWith("media/")){readLimited(zip,MAX_ENTRY_BYTES){total+=it;require(total<=MAX_PACKAGE_BYTES)};mediaNames+=n}}}}}?:error("فایل قابل خواندن نیست.");val m=requireNotNull(manifest);val p=requireNotNull(products);require(m.format==FORMAT);require(m.version<=VERSION);require(m.products==p.size);require(p.size<=MAX_PRODUCTS);require(m.images==mediaNames.size);val errs=mutableListOf<String>();val invalid=mutableSetOf<String>();val seen=mutableSetOf<String>();p.forEach{x->validateTransferProduct(x)?.let{errs+=it;invalid+=x.id};cleanSku(x.sku)?.let{if(!seen.add(normalize(it)))errs+="SKU تکراری داخل فایل: $it"};x.images.forEach{if(it.file !in mediaNames)errs+="«${x.name}»: تصویر ${it.file} در بسته موجود نیست."};x.variations.forEach{v->cleanSku(v.sku)?.let{if(!seen.add(normalize(it)))errs+="SKU تکراری داخل فایل: $it"};v.image?.let{if(it.file !in mediaNames)errs+="«${x.name}»: تصویر Variation ${it.file} در بسته موجود نیست."}}};return TransferValidatedPackage(m,p,mediaNames,invalid,errs.distinct())}

    private suspend fun uploadMediaPass(storeId:StoreId,uri:Uri,products:List<TransferProduct>,onProgress:(ProductTransferProgress)->Unit):TransferMediaOutcome=withContext(Dispatchers.IO){val destination=allMedia(storeId);val byUrl=destination.associateBy{normalize(it.src)};val byName=destination.groupBy{normalize(it.name?:it.src.substringBefore('?').substringAfterLast('/'))};val sourceImages=buildMap{products.forEach{p->p.images.forEach{put(it.file,it)};p.variations.forEach{it.image?.let{img->put(img.file,img)}}}};val result=linkedMapOf<String,ProductImage>();val errors=mutableListOf<String>();var failed=0;var uploaded=0;resolver.openInputStream(uri)?.use{input->ZipInputStream(input).use{zip->while(true){val e=zip.nextEntry?:break;if(e.isDirectory||!e.name.startsWith("media/"))continue;val file=e.name;val bytes=readLimited(zip,MAX_ENTRY_BYTES){};val src=sourceImages[file];val exact=src?.src?.let{byUrl[normalize(it)]};val candidates=byName[normalize(file.substringAfterLast('/'))].orEmpty();val reused=exact?:candidates.firstOrNull{sameBytes(it.src,bytes)};if(reused!=null)result[file]=reused else when(val r=d.uploadMedia(storeId,file.substringAfterLast('/'),bytes,mime(file))){is CoreResult.Success->{result[file]=r.value;uploaded++;onProgress(ProductTransferProgress("در حال آپلود تصاویر…",uploaded,-1))};is CoreResult.Failure->{failed++;errors+="آپلود رسانه $file ناموفق بود: ${r.error}"}}}}}}?:error("فایل قابل خواندن نیست.");TransferMediaOutcome(result,failed,errors,uploaded)}

    private suspend fun allProducts(s:StoreId,p:(ProductTransferProgress)->Unit):List<Product>{val out=mutableListOf<Product>();var page=1;while(true){val b=when(val r=d.getProducts(s,page,PAGE_SIZE,null)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت محصولات ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;p(ProductTransferProgress("در حال دریافت محصولات…",out.size,out.size));if(b.size<PAGE_SIZE)break;page++};return out.distinctBy{it.id.value}}
    private suspend fun allVars(s:StoreId,id:EntityId):List<Variation>{val out=mutableListOf<Variation>();var page=1;while(true){val b=when(val r=d.getVariations(s,id,page,PAGE_SIZE)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت Variationهای محصول ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<PAGE_SIZE)break;page++};return out.distinctBy{it.id.value}}
    private suspend fun allCategories(s:StoreId):List<IdName>{val out=mutableListOf<IdName>();var page=1;while(true){val b=when(val r=d.getProductCategories(s,page,PAGE_SIZE,null)){is CoreResult.Success->r.value;is CoreResult.Failure->break};if(b.isEmpty())break;out+=b;if(b.size<PAGE_SIZE)break;page++};return out.distinctBy{it.id.value}}
    private suspend fun allMedia(s:StoreId):List<ProductImage>{val out=mutableListOf<ProductImage>();var page=1;while(true){val b=when(val r=d.getMedia(s,page,PAGE_SIZE,null)){is CoreResult.Success->r.value;is CoreResult.Failure->break};if(b.isEmpty())break;out+=b;if(b.size<PAGE_SIZE)break;page++};return out.distinctBy{it.id.value}}
    private fun findProductMatch(x:TransferProduct,same:Boolean,byId:Map<String,Product>,bySku:Map<String,Product>,byFp:Map<String,List<Product>>):Product?{if(same)byId[x.id]?.let{return it};cleanSku(x.sku)?.let{bySku[normalize(it)]?.let{return it}};return byFp[x.matchKey()].orEmpty().singleOrNull()}
    private fun sameBytes(src:String,bytes:ByteArray):Boolean=downloadImage(src)?.contentEquals(bytes)==true
    private fun requireStore(id:StoreId)=when(val r=d.getStore(id)){is CoreResult.Success->r.value;is CoreResult.Failure->error("فروشگاه در دسترس نیست.")}
    private fun readLimited(input:ZipInputStream,max:Long,onBytes:(Long)->Unit):ByteArray{val out=ByteArrayOutputStream();val b=ByteArray(DEFAULT_BUFFER_SIZE);var total=0L;while(true){val n=input.read(b);if(n<=0)break;total+=n;require(total<=max){"یکی از فایل‌های داخل بسته بیش از حد بزرگ است."};onBytes(n.toLong());out.write(b,0,n)};return out.toByteArray()}
    private fun writeZipEntry(zip:ZipOutputStream,path:String,bytes:ByteArray){zip.putNextEntry(ZipEntry(path));zip.write(bytes);zip.closeEntry()}
    private fun downloadImage(src:String):ByteArray?=try{val c=URL(src).openConnection() as HttpURLConnection;c.connectTimeout=15_000;c.readTimeout=30_000;c.instanceFollowRedirects=true;c.inputStream.use{stream->val out=ByteArrayOutputStream();val b=ByteArray(DEFAULT_BUFFER_SIZE);var total=0L;while(true){val n=stream.read(b);if(n<=0)break;total+=n;if(total>MAX_ENTRY_BYTES)return null;out.write(b,0,n)};out.toByteArray()}.also{c.disconnect()}}catch(_:Throwable){null}
    private fun ext(s:String)=s.substringBefore('?').substringAfterLast('.','jpg').lowercase(Locale.ROOT).let{if(it in setOf("jpg","jpeg","png","webp","gif"))it else "jpg"}
    private fun mime(s:String)=when(ext(s)){"png"->"image/png";"webp"->"image/webp";"gif"->"image/gif";else->"image/jpeg"}
    private class CountingOutputStream(delegate:OutputStream,private val max:Long):FilterOutputStream(delegate){var count=0L;override fun write(b:Int){check(count+1<=max);out.write(b);count++};override fun write(b:ByteArray,off:Int,len:Int){check(count+len<=max);out.write(b,off,len);count+=len}}
}
