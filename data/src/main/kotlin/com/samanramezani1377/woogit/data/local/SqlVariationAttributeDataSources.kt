package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val localJson=Json{ignoreUnknownKeys=true;explicitNulls=false}
@Serializable private data class ImageCache(val id:Long?=null,val src:String="",val name:String?=null,val alt:String?=null)
@Serializable private data class VarAttrCache(val name:String,val option:String)
@Serializable private data class VariationCache(val id:Long,val product_id:Long,val sku:String?=null,val regular_price:String?=null,val sale_price:String?=null,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val image:ImageCache?=null,val attributes:List<VarAttrCache> = emptyList(),val date_modified_gmt:String?=null)
@Serializable private data class TermCache(val id:Long,val name:String,val slug:String?=null)

class SqlVariationDataSource(private val db:WooGitDatabase):LocalVariationDataSource{
 override fun list(storeId:StoreId,productId:EntityId):CoreResult<List<Variation>>=CoreResult.Success(db.variationQueries.selectVariationsByProduct(productId.value,storeId.value).executeAsList().map(::mapVariation))
 override fun get(storeId:StoreId,productId:EntityId,id:EntityId):CoreResult<Variation>=db.variationQueries.selectVariationById(id.value,productId.value,storeId.value).executeAsOneOrNull()?.let{CoreResult.Success(mapVariation(it))}?:CoreResult.Failure(DomainError.NotFound("variation",id.value))
 private fun mapVariation(row:com.samanramezani1377.woogit.data.db.Variation_entity):Variation{val v=localJson.decodeFromString<VariationCache>(row.payload_json);return Variation(EntityId(v.id.toString()),EntityId(v.product_id.toString()),v.attributes.map{a->VariationAttribute(a.name,a.option)},Pricing(v.regular_price,v.sale_price,v.sale_price!=null),Stock(v.stock_quantity,when(v.stock_status){"outofstock"->StockStatus.OUT_OF_STOCK;"onbackorder"->StockStatus.ON_BACKORDER;else->StockStatus.IN_STOCK},v.manage_stock),v.sku,v.image?.let{im->ProductImage(im.id?.let(::EntityId)?:EntityId("0"),im.src,im.name,im.alt)},v.date_modified_gmt?.let(Instant::parse))}
 override fun upsert(storeId:StoreId,value:Variation):CoreResult<Unit>{val payload=localJson.encodeToString(VariationCache(value.id.value.toLongOrNull()?:0L,value.productId.value.toLongOrNull()?:0L,value.sku,value.pricing.regular,value.pricing.sale,value.stock?.quantity,value.stock?.status?.name?.lowercase(),value.stock?.manageStock,value.image?.let{ImageCache(it.id?.value?.toLongOrNull(),it.src,it.name,it.alt)},value.attributes.map{VarAttrCache(it.name,it.option)},value.modifiedAt?.toString()));val now=System.currentTimeMillis();db.variationQueries.upsertVariation(value.id.value,value.productId.value,storeId.value,value.stock?.status?.name?:"",value.pricing.regular?.toLongOrNull(),value.stock?.quantity?.toInt(),payload,value.modifiedAt?.toString()?:"",value.modifiedAt?.toString()?:"",now,now);return CoreResult.Success(Unit)}
 override fun delete(storeId:StoreId,productId:EntityId,id:EntityId):CoreResult<Unit>{db.variationQueries.deleteVariation(id.value,productId.value,storeId.value);return CoreResult.Success(Unit)}
}

class SqlAttributeDataSource(private val db:WooGitDatabase):LocalAttributeDataSource{
 override fun list(storeId:StoreId):CoreResult<List<GlobalAttribute>>=CoreResult.Success(db.variationQueries.selectAttributesByStore(storeId.value).executeAsList().map{GlobalAttribute(EntityId(it.id),it.name,"",emptyList())})
 override fun get(storeId:StoreId,id:EntityId):CoreResult<GlobalAttribute>=db.variationQueries.selectAttributesByStore(storeId.value).executeAsList().firstOrNull{it.id==id.value}?.let{CoreResult.Success(GlobalAttribute(EntityId(it.id),it.name,"",emptyList()))}?:CoreResult.Failure(DomainError.NotFound("attribute",id.value))
 override fun upsert(storeId:StoreId,value:GlobalAttribute):CoreResult<Unit>{val now=System.currentTimeMillis();db.variationQueries.upsertAttribute(value.id.value,storeId.value,null,value.name,value.terms.joinToString(","){it.name},"GLOBAL","{}",now,now);return CoreResult.Success(Unit)}
 override fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit>{db.variationQueries.deleteAttribute(id.value,storeId.value);return CoreResult.Success(Unit)}
}

class SqlTermDataSource(private val db:WooGitDatabase):LocalTermDataSource{
 override fun list(storeId:StoreId,attributeId:EntityId):CoreResult<List<AttributeTerm>>=CoreResult.Success(db.variationQueries.selectTermsByAttribute(storeId.value,attributeId.value).executeAsList().map{AttributeTerm(EntityId(it.id),it.name,it.slug)})
 override fun upsert(storeId:StoreId,attributeId:EntityId,value:AttributeTerm):CoreResult<Unit>{val now=System.currentTimeMillis();val id=value.id?.value?:"local-${attributeId.value}-${value.name.hashCode()}";val payload=localJson.encodeToString(TermCache(id.toLongOrNull()?:0L,value.name,value.slug));db.variationQueries.upsertTerm(id,storeId.value,attributeId.value,value.name,value.slug,payload,now,now);return CoreResult.Success(Unit)}
 override fun delete(storeId:StoreId,attributeId:EntityId,id:EntityId):CoreResult<Unit>{db.variationQueries.deleteTerm(id.value,storeId.value,attributeId.value);return CoreResult.Success(Unit)}
}
