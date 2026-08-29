package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.Variation
import java.io.FilterOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Locale

internal fun normalize(value:String?):String=value.orEmpty().trim().lowercase(Locale.ROOT)
internal fun cleanSku(value:String?):String?=value?.trim()?.takeIf{it.isNotEmpty()}
internal fun sha256(value:ByteArray):String=MessageDigest.getInstance("SHA-256").digest(value).joinToString(""){ "%02x".format(it) }
internal fun sha256(value:String):String=sha256(value.toByteArray())
internal fun TransferProduct.matchKey():String=sha256(listOf(normalize(name),normalize(type),normalize(regular),normalize(sale),normalize(description),normalize(shortDescription),categories.map{normalize(it.name)}.sorted().joinToString(","),attributes.map{normalize(it.name)+"="+it.options.map(::normalize).sorted().joinToString(",")}.sorted().joinToString("|"),variations.map{it.attributeKey()}.sorted().joinToString("|"),normalize(quantity?.toString()),normalize(stockStatus)).joinToString("|"))
internal fun Product.matchKey():String=sha256(listOf(normalize(name),type.name.lowercase(Locale.ROOT),normalize(pricing.regular),normalize(pricing.sale),normalize(description),normalize(shortDescription),categories.map{normalize(it.name)}.sorted().joinToString(","),attributes.map{normalize(it.name)+"="+it.options.map(::normalize).sorted().joinToString(",")}.sorted().joinToString("|"),normalize(stock?.quantity?.toString()),normalize(stock?.status?.name)).joinToString("|"))
internal fun Variation.attributeKey():String=attributes.map{normalize(it.name)+"="+normalize(it.option)}.sorted().joinToString("|")
internal fun TransferVariation.attributeKey():String=attributes.map{normalize(it.name)+"="+normalize(it.option)}.sorted().joinToString("|")
internal fun nextUniqueSku(original:String?,used:Set<String>):String?{val base=cleanSku(original)?:return null;var candidate=base;while(normalize(candidate) in used)candidate="0$candidate";return candidate}
internal fun reserveNewSku(original:String?,used:MutableSet<String>,onChanged:()->Unit):String?{val unique=nextUniqueSku(original,used)?:return null;used+=normalize(unique);if(unique!=original)onChanged();return unique}
internal fun releaseSku(sku:String?,used:MutableSet<String>){sku?.let{used-=normalize(it)}}
internal class CountingOutputStream(delegate:OutputStream,private val maxBytes:Long):FilterOutputStream(delegate){var count=0L;private set;override fun write(b:Int){check(count+1<=maxBytes){"حجم فایل خروجی بیش از حد مجاز است."};out.write(b);count++}override fun write(b:ByteArray,off:Int,len:Int){check(count+len<=maxBytes){"حجم فایل خروجی بیش از حد مجاز است."};out.write(b,off,len);count+=len}}