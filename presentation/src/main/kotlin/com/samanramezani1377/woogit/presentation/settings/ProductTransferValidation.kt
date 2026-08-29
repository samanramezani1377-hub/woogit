package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.zip.ZipInputStream

private const val MAX_PRODUCTS=10_000
private const val MAX_PACKAGE_BYTES=1_073_741_824L
private const val MAX_ENTRY_BYTES=50L*1024L*1024L
private const val MAX_PRODUCT_JSON_BYTES=100L*1024L*1024L

internal fun validateTransferProduct(x:TransferProduct):String?=when{
    x.id.isBlank()->"محصول بدون شناسه است و وارد نشد."
    x.name.isBlank()->"محصول با شناسه ${x.id} فاقد نام است و وارد نشد."
    x.type.isBlank()->"محصول «${x.name}» فاقد نوع است و وارد نشد."
    x.status.isBlank()->"محصول «${x.name}» فاقد وضعیت است و وارد نشد."
    runCatching{ProductType.valueOf(x.type)}.isFailure->"نوع محصول «${x.type}» برای «${x.name}» معتبر نیست."
    runCatching{ProductStatus.valueOf(x.status)}.isFailure->"وضعیت محصول «${x.status}» برای «${x.name}» معتبر نیست."
    x.variations.any{it.attributes.any{a->a.name.isBlank()||a.option.isBlank()}}->"Variationهای محصول «${x.name}» دارای Attribute نامعتبر هستند."
    else->null
}

internal fun validateTransferPackage(resolver:ContentResolver,uri:Uri,json:Json):TransferValidatedPackage{
    var manifest:ProductTransferManifest?=null;var products:List<TransferProduct>?=null;var globals:List<TransferGlobalAttribute> = emptyList();val mediaNames=linkedSetOf<String>();val seenEntries=mutableSetOf<String>();var total=0L
    resolver.openInputStream(uri)?.use{input->ZipInputStream(input).use{zip->while(true){val entry=zip.nextEntry?:break;if(entry.isDirectory)continue;val name=entry.name;require(name.isNotBlank()&&!name.startsWith("/")&&!name.startsWith("\\")&&!name.split('/','\\').any{it==".."}){"مسیر نامعتبر داخل فایل WooGit."};require(seenEntries.add(name)){"ورودی تکراری داخل فایل WooGit: $name"};val b=readTransferEntry(zip,if(name==ProductTransferFormat.PRODUCTS_ENTRY)MAX_PRODUCT_JSON_BYTES else MAX_ENTRY_BYTES);total+=b.size;require(total<=MAX_PACKAGE_BYTES){"حجم فایل WooGit بیش از حد مجاز است."};when(name){ProductTransferFormat.MANIFEST_ENTRY->manifest=json.decodeFromString(b.toString(Charsets.UTF_8));ProductTransferFormat.PRODUCTS_ENTRY->{val pack=json.decodeFromString<ProductTransferPackage>(b.toString(Charsets.UTF_8));manifest=manifest?:pack.manifest;products=pack.products;globals=pack.globalAttributes};else->mediaNames+=name}}}}?:error("فایل قابل خواندن نیست.")
    val m=requireNotNull(manifest){"manifest.json در فایل وجود ندارد."};val p=requireNotNull(products){"products.json در فایل وجود ندارد."};require(m.format==ProductTransferFormat.FORMAT){"فرمت فایل WooGit معتبر نیست."};require(m.version in ProductTransferFormat.SUPPORTED_FORMAT_VERSIONS){"نسخه ساختاری فایل WooGit پشتیبانی نمی‌شود: ${m.version}"};val layout=ProductTransferFormat.layoutForImport(m.layoutVersion);require(mediaNames.all(layout::acceptsEntryPath)){"یک یا چند ورودی با Layout v${layout.version} سازگار نیستند."};require(m.products==p.size){"تعداد محصولات فایل با manifest سازگار نیست."};require(p.size<=MAX_PRODUCTS){"تعداد محصولات فایل بیش از حد مجاز است."};require(m.images==mediaNames.size){"تعداد تصاویر فایل با manifest سازگار نیست."}
    val errors=mutableListOf<String>();val invalid=mutableSetOf<String>();val seenSku=mutableMapOf<String,String>();val seenProductIds=mutableSetOf<String>();val globalIds=mutableSetOf<String>();globals.forEach{g->if(g.id.isBlank()||g.name.isBlank()||g.slug.isBlank())errors+="ویژگی سراسری ناقص است: ${g.id}";if(!globalIds.add(g.id))errors+="ویژگی سراسری تکراری داخل فایل: ${g.id}";val termNames=mutableSetOf<String>();g.terms.forEach{t->if(t.name.isBlank())errors+="Term بدون نام برای ویژگی ${g.name}";if(!termNames.add(normalize(t.name)))errors+="Term تکراری داخل ویژگی ${g.name}: ${t.name}"}}
    p.forEach{x->{validateTransferProduct(x)?.let{errors+=it;invalid+=x.id};if(!seenProductIds.add(x.id))errors+="شناسه محصول تکراری داخل فایل: ${x.id}";cleanSku(x.sku)?.let{val key=normalize(it);val previous=seenSku.putIfAbsent(key,x.id);if(previous!=null)errors+="SKU تکراری داخل فایل: $it (${previous} / ${x.id})"};x.images.forEach{if(it.file !in mediaNames)errors+="«${x.name}»: تصویر ${it.file} در بسته موجود نیست."};x.variations.forEach{v->{cleanSku(v.sku)?.let{val key=normalize(it);val previous=seenSku.putIfAbsent(key,"${x.id}/${v.id}");if(previous!=null)errors+="SKU تکراری داخل فایل: $it (${previous} / ${x.id}/${v.id})"};v.image?.let{if(it.file !in mediaNames)errors+="«${x.name}»: تصویر Variation ${it.file} در بسته موجود نیست."}}}}
    return TransferValidatedPackage(m,p,mediaNames,invalid,errors.distinct(),globals)
}