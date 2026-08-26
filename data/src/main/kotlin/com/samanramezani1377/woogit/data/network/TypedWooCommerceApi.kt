package com.samanramezani1377.woogit.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val typedJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

@Serializable data class WooAddressDto(val first_name:String?=null,val last_name:String?=null,val company:String?=null,val address_1:String?=null,val address_2:String?=null,val city:String?=null,val state:String?=null,val postcode:String?=null,val country:String?=null,val phone:String?=null)
@Serializable data class WooLineItemDto(val id:Long,val name:String="",val product_id:Long=0,val variation_id:Long=0,val quantity:Double=0.0,val subtotal:String="0",val total:String="0")
@Serializable data class WooShippingLineDto(val id:Long=0,val method_id:String?=null,val method_title:String?=null,val total:String?=null)
@Serializable data class WooCouponLineDto(val code:String="",val discount:String="0")
@Serializable data class WooOrderTypedDto(val id:Long,val number:String="",val status:String="",val total:String="0",val currency:String="",val customer_id:Long=0,val billing:WooAddressDto?=null,val shipping:WooAddressDto?=null,val payment_method:String?=null,val payment_method_title:String?=null,val transaction_id:String?=null,val date_modified_gmt:String?=null,val line_items:List<WooLineItemDto> = emptyList(),val shipping_lines:List<WooShippingLineDto> = emptyList(),val coupon_lines:List<WooCouponLineDto> = emptyList())
@Serializable data class WooOrderNoteDto(val id:Long=0,val note:String,val customer_note:Boolean=false,val date_created_gmt:String?=null)
@Serializable data class WooImageTypedDto(val id:Long?=null,val src:String="",val name:String?=null,val alt:String?=null)
@Serializable data class WooCategoryDto(val id:Long=0,val name:String="")
@Serializable data class WooProductAttributeDto(val id:Long?=null,val name:String="",val visible:Boolean=true,val variation:Boolean=false,val options:List<String> = emptyList())
@Serializable data class WooProductTypedDto(val id:Long,val name:String="",val slug:String?=null,val sku:String?=null,val description:String?=null,val short_description:String?=null,val status:String="draft",val type:String="simple",val price:String?=null,val regular_price:String?=null,val sale_price:String?=null,val on_sale:Boolean=false,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val images:List<WooImageTypedDto> = emptyList(),val categories:List<WooCategoryDto> = emptyList(),val attributes:List<WooProductAttributeDto> = emptyList(),val date_modified_gmt:String?=null)
@Serializable data class WooVariationTypedDto(val id:Long,val product_id:Long=0,val sku:String?=null,val price:String?=null,val regular_price:String?=null,val sale_price:String?=null,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val image:WooImageTypedDto?=null,val date_modified_gmt:String?=null,val attributes:List<WooProductAttributeDto> = emptyList())
@Serializable data class WooGlobalAttributeDto(val id:Long,val name:String="",val slug:String="")
@Serializable data class WooAttributeTermDto(val id:Long,val name:String="",val slug:String="")
@Serializable data class WooSystemStatusDto(val environment:Map<String,String> = emptyMap())

class TypedWooCommerceApi(private val raw: WooCommerceApi) {
    private fun <T> decode(response:ApiResponse, decoder:(String)->T): Result<T> = if(response.statusCode in 200..299) runCatching{decoder(response.body)} else Result.failure(HttpApiException(response.statusCode,response.body))
    suspend fun validate(baseUrl:String):Result<WooSystemStatusDto> = decode(raw.validateStore(baseUrl)){typedJson.decodeFromString<WooSystemStatusDto>(it)}
    suspend fun orders(baseUrl:String,page:Int,perPage:Int,search:String?,status:String?):Result<List<WooOrderTypedDto>> = decode(raw.listOrders(baseUrl,page,perPage,search,status)){typedJson.decodeFromString(it)}
    suspend fun order(baseUrl:String,id:Long):Result<WooOrderTypedDto> = decode(raw.getOrder(baseUrl,id)){typedJson.decodeFromString(it)}
    suspend fun updateOrder(baseUrl:String,id:Long,order:WooOrderTypedDto):Result<WooOrderTypedDto> = decode(raw.updateOrder(baseUrl,id,typedJson.encodeToString(order))){typedJson.decodeFromString(it)}
    suspend fun addOrderNote(baseUrl:String,orderId:Long,note:WooOrderNoteDto):Result<WooOrderNoteDto> = decode(raw.addOrderNote(baseUrl,orderId,typedJson.encodeToString(note))){typedJson.decodeFromString(it)}
    suspend fun products(baseUrl:String,page:Int,perPage:Int,search:String?):Result<List<WooProductTypedDto>> = decode(raw.listProducts(baseUrl,page,perPage,search)){typedJson.decodeFromString(it)}
    suspend fun product(baseUrl:String,id:Long):Result<WooProductTypedDto> = decode(raw.getProduct(baseUrl,id)){typedJson.decodeFromString(it)}
    suspend fun createProduct(baseUrl:String,product:WooProductTypedDto):Result<WooProductTypedDto> = decode(raw.createProduct(baseUrl,typedJson.encodeToString(product))){typedJson.decodeFromString(it)}
    suspend fun updateProduct(baseUrl:String,id:Long,product:WooProductTypedDto):Result<WooProductTypedDto> = decode(raw.updateProduct(baseUrl,id,typedJson.encodeToString(product))){typedJson.decodeFromString(it)}
    suspend fun deleteProduct(baseUrl:String,id:Long):Result<Unit> = decode(raw.deleteProduct(baseUrl,id,true)){Unit}
    suspend fun variations(baseUrl:String,productId:Long,page:Int,perPage:Int):Result<List<WooVariationTypedDto>> = decode(raw.listVariations(baseUrl,productId,page,perPage)){typedJson.decodeFromString(it)}
    suspend fun variation(baseUrl:String,productId:Long,id:Long):Result<WooVariationTypedDto> = decode(raw.getVariation(baseUrl,productId,id)){typedJson.decodeFromString(it)}
    suspend fun createVariation(baseUrl:String,productId:Long,value:WooVariationTypedDto):Result<WooVariationTypedDto> = decode(raw.createVariation(baseUrl,productId,typedJson.encodeToString(value))){typedJson.decodeFromString(it)}
    suspend fun updateVariation(baseUrl:String,productId:Long,id:Long,value:WooVariationTypedDto):Result<WooVariationTypedDto> = decode(raw.updateVariation(baseUrl,productId,id,typedJson.encodeToString(value))){typedJson.decodeFromString(it)}
    suspend fun deleteVariation(baseUrl:String,productId:Long,id:Long):Result<Unit> = decode(raw.deleteVariation(baseUrl,productId,id,true)){Unit}
    suspend fun attributes(baseUrl:String,page:Int,perPage:Int):Result<List<WooGlobalAttributeDto>> = decode(raw.listAttributes(baseUrl,page,perPage)){typedJson.decodeFromString(it)}
    suspend fun attribute(baseUrl:String,id:Long):Result<WooGlobalAttributeDto> = decode(raw.getAttribute(baseUrl,id)){typedJson.decodeFromString(it)}
    suspend fun createAttribute(baseUrl:String,value:WooGlobalAttributeDto):Result<WooGlobalAttributeDto> = decode(raw.createAttribute(baseUrl,typedJson.encodeToString(value))){typedJson.decodeFromString(it)}
    suspend fun updateAttribute(baseUrl:String,id:Long,value:WooGlobalAttributeDto):Result<WooGlobalAttributeDto> = decode(raw.updateAttribute(baseUrl,id,typedJson.encodeToString(value))){typedJson.decodeFromString(it)}
    suspend fun deleteAttribute(baseUrl:String,id:Long):Result<Unit> = decode(raw.deleteAttribute(baseUrl,id,true)){Unit}
    suspend fun terms(baseUrl:String,attributeId:Long,page:Int,perPage:Int):Result<List<WooAttributeTermDto>> = decode(raw.listAttributeTerms(baseUrl,attributeId,page,perPage)){typedJson.decodeFromString(it)}
    suspend fun term(baseUrl:String,attributeId:Long,id:Long):Result<WooAttributeTermDto> = decode(raw.getAttributeTerm(baseUrl,attributeId,id)){typedJson.decodeFromString(it)}
    suspend fun createTerm(baseUrl:String,attributeId:Long,value:WooAttributeTermDto):Result<WooAttributeTermDto> = decode(raw.createAttributeTerm(baseUrl,attributeId,typedJson.encodeToString(value))){typedJson.decodeFromString(it)}
    suspend fun updateTerm(baseUrl:String,attributeId:Long,id:Long,value:WooAttributeTermDto):Result<WooAttributeTermDto> = decode(raw.updateAttributeTerm(baseUrl,attributeId,id,typedJson.encodeToString(value))){typedJson.decodeFromString(it)}
    suspend fun deleteTerm(baseUrl:String,attributeId:Long,id:Long):Result<Unit> = decode(raw.deleteAttributeTerm(baseUrl,attributeId,id,true)){Unit}
}

data class HttpApiException(val statusCode:Int,val body:String):Exception("WooCommerce HTTP $statusCode")
