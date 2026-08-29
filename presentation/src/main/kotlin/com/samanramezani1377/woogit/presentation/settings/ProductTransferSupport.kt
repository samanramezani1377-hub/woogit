package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.model.*
import java.security.MessageDigest
import java.util.Locale

internal fun normalize(value:String?):String=value.orEmpty().trim().lowercase(Locale.ROOT)
internal fun cleanSku(value:String?):String?=value?.trim()?.takeIf{it.isNotEmpty()}
internal fun sha256(value:String):String=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){ "%02x".format(it) }
internal fun TransferProduct.matchKey():String=sha256(listOf(normalize(name),normalize(type),normalize(regular),normalize(sale),normalize(description),normalize(shortDescription)).joinToString("|"))
internal fun Product.matchKey():String=sha256(listOf(normalize(name),type.name.lowercase(Locale.ROOT),normalize(pricing.regular),normalize(pricing.sale),normalize(description),normalize(shortDescription)).joinToString("|"))
internal fun Variation.attributeKey():String=attributes.map{normalize(it.name)+"="+normalize(it.option)}.sorted().joinToString("|")
internal fun TransferVariation.attributeKey():String=attributes.map{normalize(it.name)+"="+normalize(it.option)}.sorted().joinToString("|")
internal fun nextUniqueSku(original:String?,used:Set<String>):String?{val base=cleanSku(original)?:return null;var candidate=base;while(normalize(candidate) in used)candidate="0$candidate";return candidate}
internal fun reserveNewSku(original:String?,used:MutableSet<String>,onChanged:()->Unit):String?{val unique=nextUniqueSku(original,used)?:return null;used+=normalize(unique);if(!unique.equals(original,false))onChanged();return unique}
internal fun releaseSku(sku:String?,used:MutableSet<String>){sku?.let{used-=normalize(it)}}