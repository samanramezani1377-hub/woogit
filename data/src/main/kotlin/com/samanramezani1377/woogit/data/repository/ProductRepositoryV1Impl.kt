package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.network.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

private val productJson=Json{ignoreUnknownKeys=true;explicitNulls=false}
private fun digest(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){ "%02x".format(it) }
@Serializable private data class ProductMutationV1(val name:String,val sku:String?=null,val description:String?=null,val short_description:String?=null,val status:String,val type:String,val regular_price:String?=null,val sale_price:String?=null,val images:List<WooImageTypedDto> = emptyList(),val attributes:List<WooProductAttributeDto> = emptyList())

class ProductRepositoryV1Impl(private val local:LocalProductDataSource<Product>,private val provider:WooCommerceClientProvider,private val pending:PendingOperationRepository):ProductRepository{
 private val base=ProductRepositoryImpl(local,provider,pending)
 override suspend fun get(storeId:StoreId,id:EntityId)=base.get(storeId,id)
 override suspend fun list(storeId:StoreId,page:Int,perPage:Int,search:String?)=base.list(storeId,page,perPage,search)
 override suspend fun create(storeId:StoreId,product:Product)=base.create(storeId,product)
 override suspend fun delete(storeId:StoreId,id:EntityId)=base.delete(storeId,id)
 override suspend fun update(storeId:StoreId,id:EntityId,product:Product):CoreResult<Product>{
  local.upsert(storeId,product);val payload=productJson.encodeToString(ProductMutationV1(product.name,product.sku,product.description,product.shortDescription,product.status.name.lowercase(),product.type.name.lowercase(),product.pricing.regular,product.pricing.sale,product.images.map{WooImageTypedDto(it.id?.value?.toLong(),it.src,it.name,it.alt)},product.attributes.map{WooProductAttributeDto(it.id?.value?.toLong(),it.name,it.visible,it.variation,it.options)}));val hash=digest(payload);val op=PendingOperation(EntityId("product-update-${storeId.value}-${id.value}-${hash.take(16)}"),storeId,"product",id,OperationType.UPDATE,payload,hash,0,null);pending.enqueue(op);return provider.client(storeId).fold({(store,api)->api.updateProduct(store.baseUrl,id.value.toLong(),WooProductTypedDto(id.value.toLong(),product.name,sku=product.sku,description=product.description,short_description=product.shortDescription,status=product.status.name.lowercase(),type=product.type.name.lowercase(),regular_price=product.pricing.regular,sale_price=product.pricing.sale,images=product.images.map{WooImageTypedDto(it.id?.value?.toLong(),it.src,it.name,it.alt)},attributes=product.attributes.map{WooProductAttributeDto(it.id?.value?.toLong(),it.name,it.visible,it.variation,it.options)})).fold({remote->pending.markSucceeded(op.id);CoreResult.Success(product)}, {CoreResult.Success(product)})},{CoreResult.Success(product)})
 }
}
