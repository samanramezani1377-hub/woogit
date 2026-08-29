package com.samanramezani1377.woogit.presentation.settings

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val MAX_ENTRY_BYTES=50L*1024L*1024L

internal fun readTransferEntry(zip:ZipInputStream,maxBytes:Long):ByteArray{val out=ByteArrayOutputStream();val buffer=ByteArray(DEFAULT_BUFFER_SIZE);var total=0L;while(true){val n=zip.read(buffer);if(n<=0)break;total+=n;require(total<=maxBytes){"یکی از فایل‌های داخل بسته بیش از حد بزرگ است."};out.write(buffer,0,n)};return out.toByteArray()}
internal fun writeTransferEntry(zip:ZipOutputStream,path:String,bytes:ByteArray){zip.putNextEntry(ZipEntry(path));zip.write(bytes);zip.closeEntry()}
internal fun downloadTransferImage(src:String):ByteArray?=try{val connection=URL(src).openConnection() as HttpURLConnection;connection.connectTimeout=15000;connection.readTimeout=30000;connection.instanceFollowRedirects=true;connection.inputStream.use{stream->val out=ByteArrayOutputStream();val buffer=ByteArray(DEFAULT_BUFFER_SIZE);var total=0L;while(true){val n=stream.read(buffer);if(n<=0)break;total+=n;if(total>MAX_ENTRY_BYTES)return null;out.write(buffer,0,n)};out.toByteArray()}.also{connection.disconnect()}}catch(_:Throwable){null}
internal fun transferExt(src:String):String=src.substringBefore('?').substringAfterLast('.','jpg').lowercase(Locale.ROOT).let{if(it in setOf("jpg","jpeg","png","webp","gif","svg"))it else "jpg"}
internal fun transferMime(src:String):String=when(transferExt(src)){"png"->"image/png";"webp"->"image/webp";"gif"->"image/gif";"svg"->"image/svg+xml";else->"image/jpeg"}
