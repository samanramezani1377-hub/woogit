package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipInputStream

private const val MEDIA_MAX_BYTES=50L*1024L*1024L
private const val BUFFER=8192

internal class ProductTransferMedia(private val d:V1PresentationDependencies,private val resolver:ContentResolver){
    suspend fun upload(storeId:StoreId,source:Uri,products:List<TransferProduct>,onProgress:(ProductTransferProgress)->Unit):TransferMediaOutcome=withContext(Dispatchers.IO){
        val destination=readDestinationMedia(storeId)
        val byUrl=destination.mapNotNull{it.src.takeIf(String::isNotBlank)?.let(normalize to it)}.toMap()
        val byName=destination.groupBy{normalize(fileName(it.name?:it.src))}
        val sourceByFile=buildMap<String,TransferImage>{products.forEach{p->p.images.forEach{put(it.file,it)};p.variations.forEach{it.image?.let(img->put(img.file,img))}}}
        val resolved=linkedMapOf<String,ProductImage>();val errors=mutableListOf<String>();var failed=0;var uploaded=0
        resolver.openInputStream(source)?.use{input->ZipInputStream(input).use{zip->while(true){val entry=zip.nextEntry?:break;if(entry.isDirectory||!entry.name.startsWith("media/"))continue;val file=entry.name;val bytes=readLimited(zip);val sourceImage=sourceByFile[file];val exact=sourceImage?.src?.let{byUrl[normalize(it)]};val byFilename=byName[normalize(fileName(sourceImage?.name?:file))].orEmpty();val reused=exact?:byFilename.firstOrNull{remoteBytes(it.src)?.let{b->b.contentEquals(bytes)}==true};if(reused!=null){resolved[file]=reused}else{when(val r=d.uploadMedia(storeId,fileName(file),bytes,mime(file))){is CoreResult.Success->{resolved[file]=r.value;uploaded++;onProgress(ProductTransferProgress("در حال آپلود تصاویر…",uploaded,-1))};is CoreResult.Failure->{failed++;errors+="آپلود رسانه $file ناموفق بود: ${r.error}"}}}}}}?:error("فایل قابل خواندن نیست.")
        TransferMediaOutcome(resolved,failed,errors,uploaded)
    }
    private suspend fun readDestinationMedia(storeId:StoreId):List<ProductImage>=ProductTransferRepositoryReader(d).media(storeId)
    private fun readLimited(zip:ZipInputStream):ByteArray{val out=ByteArrayOutputStream();val buffer=ByteArray(BUFFER);var total=0L;while(true){val n=zip.read(buffer);if(n<=0)break;total+=n;require(total<=MEDIA_MAX_BYTES){"یکی از تصاویر بیش از حد مجاز است."};out.write(buffer,0,n)};return out.toByteArray()}
    private fun remoteBytes(src:String):ByteArray?=try{val c=URL(src).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;c.instanceFollowRedirects=true;c.inputStream.use{input->val out=ByteArrayOutputStream();val buffer=ByteArray(BUFFER);var total=0L;while(true){val n=input.read(buffer);if(n<=0)break;total+=n;if(total>MEDIA_MAX_BYTES)return null;out.write(buffer,0,n)};out.toByteArray()}.also{c.disconnect()}}catch(_:Throwable){null}
    private fun fileName(value:String):String=value.substringBefore('?').substringAfterLast('/').trim()
    private fun mime(value:String):String=when(value.substringBefore('?').substringAfterLast('.').lowercase(Locale.ROOT)){"png"->"image/png";"webp"->"image/webp";"gif"->"image/gif";"svg"->"image/svg+xml";else->"image/jpeg"}
}