package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val localJson=Json{ignoreUnknownKeys=true;explicitNulls=false}
@Serializable private data class ImageCache(val id:Long?=null,val src:String="",val name:String?=null,val alt:String?=null)
@Serializable private data class VarAttrCache(val name:String,val option:String)
@Serializable private data class VariationCache(val id:Long,val product_id:Long,val sku:String?=null,val regular_price:String?=null,val sale_price:String?=null,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val image:ImageCache?=null,val attributes:List<VarAttrCache> = emptyList())

class SqlVariationDataSource(private val db:WooGitDatabase){
 fun list(storeId:StoreId,productId:EntityId):List<Variation>=db.variationQueries.selectVariationsByProduct(productId.value).executeAsList().filter{it.store_id==storeId.value}.map{val v=localJson.decodeFromString<VariationCache>(it.payload_json);Variation(EntityId(v.id.toString()),EntityId(v.product_id.toString()),v.attributes.map{a->VariationAttribute(a.name,a.option)},Pricing(v.regular_price,v.sale_price,v.sale_price!=null),Stock(v.stock_quantity,when(v.stock_status){"outofstock"->StockStatus.OUT_OF_STOCK;"onbackorder"->StockStatus.ON_BACKORDER;else->StockStatus.IN_STOCK},v.manage_stock),v.sku,v.image?.let{im->ProductImage(im.id?.let(::EntityId)?:EntityId("0"),im.src,im.name,im.alt)})}
 fun upsert(storeId:StoreId,value:Variation,payload:String,version:String="",modifiedAt:String=""){val now=System.currentTimeMillis();db.variationQueries.upsertVariation(value.id.value,value.productId.value,storeId.value,value.stock?.status?.name?:"",value.pricing.regular?.toLongOrNull(),value.stock?.quantity?.toInt(),payload,version,modifiedAt,now,now)}
 fun delete(id:EntityId){db.variationQueries.deleteVariation(id.value)}
}

class SqlAttributeDataSource(private val db:WooGitDatabase){
 fun list(storeId:StoreId,productId:EntityId):List<Attribute>=db.variationQueries.selectAttributesByProduct(storeId.value,productId.value).executeAsList().map{Attribute(it.id.toLongOrNull()?.let(::EntityId),it.name,true,it.scope=="VARIATION",it.value.split(",").filter(String::isNotBlank))}
 fun listStore(storeId:StoreId):List<Attribute>=db.variationQueries.selectAttributesByStore(storeId.value).executeAsList().map{Attribute(it.id.toLongOrNull()?.let(::EntityId),it.name,true,it.scope=="VARIATION",it.value.split(",").filter(String::isNotBlank))}
 fun upsert(storeId:StoreId,productId:EntityId?,attribute:Attribute){val now=System.currentTimeMillis();val id=attribute.id?.value?:"local-${attribute.name.hashCode()}";db.variationQueries.upsertAttribute(id,storeId.value,productId?.value,attribute.name,attribute.options.joinToString(","),if(attribute.variation)"VARIATION"else"CUSTOM","{}",now,now)}
 fun delete(id:String){db.variationQueries.deleteAttribute(id)}
}
