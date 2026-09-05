package com.samanramezani1377.woogit.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

private val typedJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

@Serializable data class WooAddressDto(val first_name:String?=null,val last_name:String?=null,val company:String?=null,val address_1:String?=null,val address_2:String?=null,val city:String?=null,val state:String?=null,val postcode:String?=null,val country:String?=null,val phone:String?=null)
@Serializable data class WooLineItemDto(val id:Long,val name:String="",val product_id:Long=0,val variation_id:Long=0,val quantity:Double=0.0,val subtotal:String="0",val total:String="0")
@Serializable data class WooShippingLineDto(val id:Long=0,val method_id:String?=null,val method_title:String?=null,val total:String?=null)
@Serializable data class WooCouponLineDto(val code:String="",val discount:String="0")
@Serializable data class WooOrderTypedDto(val id:Long,val number:String="",val status:String="",val total:String="0",val currency:String="",val customer_id:Long=0,val billing:WooAddressDto?=null,val shipping:WooAddressDto?=null,val payment_method:String?=null,val payment_method_title:String?=null,val transaction_id:String?=null,val set_paid:Boolean?=null,val date_paid_gmt:String?=null,val date_modified_gmt:String?=null,val line_items:List<WooLineItemDto> = emptyList(),val shipping_lines:List<WooShippingLineDto> = emptyList(),val coupon_lines:List<WooCouponLineDto> = emptyList())
@Serializable data class WooOrderNoteDto(val id:Long=0,val note:String,val customer_note:Boolean=false,val date_created_gmt:String?=null)
@Serializable data class WooImageTypedDto(val id:Long?=null,val src:String?=null,val name:String?=null,val alt:String?=null)
@Serializable data class WooCategoryDto(val id:Long=0,val name:String="",val parent:Long=0)
@Serializable data class WooProductAttributeDto(val id:Long?=null,val name:String="",val visible:Boolean=true,val variation:Boolean=false,val options:List<String> = emptyList())
@Serializable data class WooVariationAttributeDto(val id:Long?=null,val name:String="",val option:String="")
@Serializable data class WooMetaDataDto(val id:Long=0,val key:String="",val value:JsonElement?=null)
@Serializable data class WooProductTypedDto(val id:Long,val name:String="",val slug:String?=null,val sku:String?=null,val status:String="draft",val type:String="simple",val price:String?=null,val regular_price:String?=null,val sale_price:String?=null,val on_sale:Boolean=false,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val images:List<WooImageTypedDto> = emptyList(),val categories:List<WooCategoryDto> = emptyList(),val attributes:List<WooProductAttributeDto> = emptyList(),val meta_data:List<WooMetaDataDto> = emptyList(),val date_modified_gmt:String?=null,val description:String?=null,val short_description:String?=null)
@Serializable data class WooVariationTypedDto(val id:Long,val product_id:Long=0,val sku:String?=null,val price:String?=null,val regular_price:String?=null,val sale_price:String?=null,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val image:WooImageTypedDto?=null,val date_modified_gmt:String?=null,val attributes:List<WooVariationAttributeDto> = emptyList())
@Serializable data class WooGlobalAttributeDto(val id:Long,val name:String="",val slug:String="")
@Serializable data class WooAttributeTermDto(val id:Long,val name:String="",val slug:String="")

@Serializable data class WooSystemStatusDto(val environment: Map<String, JsonElement> = emptyMap(), val settings: WooSystemStatusSettingsDto = WooSystemStatusSettingsDto()) {
    fun environmentString(key: String): String? = environment[key]?.asText()
    fun environmentBoolean(key: String): Boolean? = environment[key]?.asBoolean()
    fun environmentInt(key: String): Int? = environment[key]?.asInt()
}
private fun JsonElement.asText(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
private fun JsonElement.asInt(): Int? = (this as? JsonPrimitive)?.intOrNull

@Serializable data class WooMediaTitleDto(val rendered:String="")
@Serializable data class WooMediaDto(val id:Long,val source_url:String="",val title:WooMediaTitleDto?=null,val alt_text:String="")

class TypedWooCommerceApi(private val raw: WooCommerceApi) {
    private fun <T> decode(r: ApiResponse, decoder: (String) -> T): Result<T> = if (r.statusCode in 200..299) runCatching { decoder(r.body) } else Result.failure(HttpApiException(r.statusCode, r.body))
    suspend fun validate(b:String) = decode(raw.validateStore(b)) { typedJson.decodeFromString<WooSystemStatusDto>(it) }
    suspend fun orders(b:String,p:Int,n:Int,s:String?,st:String?) = decode(raw.listOrders(b,p,n,s,st)) { typedJson.decodeFromString<List<WooOrderTypedDto>>(it) }
    suspend fun order(b:String,id:Long) = decode(raw.getOrder(b,id)) { typedJson.decodeFromString<WooOrderTypedDto>(it) }
    suspend fun updateOrder(b:String,id:Long,o:WooOrderTypedDto) = decode(raw.updateOrder(b,id,typedJson.encodeToString(o))) { typedJson.decodeFromString<WooOrderTypedDto>(it) }
    suspend fun addOrderNote(b:String,id:Long,n:WooOrderNoteDto) = decode(raw.addOrderNote(b,id,typedJson.encodeToString(n))) { typedJson.decodeFromString<WooOrderNoteDto>(it) }
    suspend fun salesReport(b:String,dateMin:String,dateMax:String) = decode(raw.salesReport(b,dateMin,dateMax)) { typedJson.decodeFromString<List<WooSalesReportDto>>(it).firstOrNull() ?: WooSalesReportDto() }
    suspend fun products(b:String,p:Int,n:Int,s:String?,modifiedAfter:String?=null) = decode(raw.listProducts(b,p,n,s,modifiedAfter)) { typedJson.decodeFromString<List<WooProductTypedDto>>(it) }
    suspend fun product(b:String,id:Long) = decode(raw.getProduct(b,id)) { typedJson.decodeFromString<WooProductTypedDto>(it) }
    suspend fun createProduct(b:String,p:WooProductTypedDto) = decode(raw.createProduct(b,typedJson.encodeToString(p))) { typedJson.decodeFromString<WooProductTypedDto>(it) }
    suspend fun updateProduct(b:String,id:Long,p:WooProductTypedDto) = decode(raw.updateProduct(b,id,typedJson.encodeToString(p))) { typedJson.decodeFromString<WooProductTypedDto>(it) }
    suspend fun updateProductFields(b:String,id:Long,fields:JsonObject) = decode(raw.updateProduct(b,id,fields.toString())) { typedJson.decodeFromString<WooProductTypedDto>(it) }
    suspend fun deleteProduct(b:String,id:Long) = decode(raw.deleteProduct(b,id,true)) { Unit }
    suspend fun productCategories(b:String,p:Int,n:Int,s:String?) = decode(raw.listProductCategories(b,p,n,s)) { typedJson.decodeFromString<List<WooCategoryDto>>(it) }
    suspend fun createProductCategory(b:String,c:WooCategoryDto) = decode(raw.createProductCategory(b,typedJson.encodeToString(c))) { typedJson.decodeFromString<WooCategoryDto>(it) }
    suspend fun variations(b:String,p:Long,n:Int,c:Int) = decode(raw.listVariations(b,p,n,c)) { typedJson.decodeFromString<List<WooVariationTypedDto>>(it) }
    suspend fun variation(b:String,p:Long,id:Long) = decode(raw.getVariation(b,p,id)) { typedJson.decodeFromString<WooVariationTypedDto>(it) }
    suspend fun createVariation(b:String,p:Long,v:WooVariationTypedDto) = decode(raw.createVariation(b,p,typedJson.encodeToString(v))) { typedJson.decodeFromString<WooVariationTypedDto>(it) }
    suspend fun updateVariation(b:String,p:Long,id:Long,v:WooVariationTypedDto) = decode(raw.updateVariation(b,p,id,typedJson.encodeToString(v))) { typedJson.decodeFromString<WooVariationTypedDto>(it) }
    suspend fun deleteVariation(b:String,p:Long,id:Long) = decode(raw.deleteVariation(b,p,id,true)) { Unit }
    suspend fun attributes(b:String,p:Int,n:Int) = decode(raw.listAttributes(b,p,n)) { typedJson.decodeFromString<List<WooGlobalAttributeDto>>(it) }
    suspend fun attribute(b:String,id:Long) = decode(raw.getAttribute(b,id)) { typedJson.decodeFromString<WooGlobalAttributeDto>(it) }
    suspend fun createAttribute(b:String,v:WooGlobalAttributeDto) = decode(raw.createAttribute(b,typedJson.encodeToString(v))) { typedJson.decodeFromString<WooGlobalAttributeDto>(it) }
    suspend fun updateAttribute(b:String,id:Long,v:WooGlobalAttributeDto) = decode(raw.updateAttribute(b,id,typedJson.encodeToString(v))) { typedJson.decodeFromString<WooGlobalAttributeDto>(it) }
    suspend fun deleteAttribute(b:String,id:Long) = decode(raw.deleteAttribute(b,id,true)) { Unit }
    suspend fun terms(b:String,a:Long,p:Int,n:Int) = decode(raw.listAttributeTerms(b,a,p,n)) { typedJson.decodeFromString<List<WooAttributeTermDto>>(it) }
    suspend fun term(b:String,a:Long,id:Long) = decode(raw.getAttributeTerm(b,a,id)) { typedJson.decodeFromString<WooAttributeTermDto>(it) }
    suspend fun createTerm(b:String,a:Long,v:WooAttributeTermDto) = decode(raw.createAttributeTerm(b,a,typedJson.encodeToString(v))) { typedJson.decodeFromString<WooAttributeTermDto>(it) }
    suspend fun updateTerm(b:String,a:Long,id:Long,v:WooAttributeTermDto) = decode(raw.updateAttributeTerm(b,a,id,typedJson.encodeToString(v))) { typedJson.decodeFromString<WooAttributeTermDto>(it) }
    suspend fun deleteTerm(b:String,a:Long,id:Long) = decode(raw.deleteAttributeTerm(b,a,id,true)) { Unit }
    suspend fun media(b:String,p:Int,n:Int,s:String?) = decode(raw.listMedia(b,p,n,s)) { typedJson.decodeFromString<List<WooMediaDto>>(it) }
    suspend fun downloadMedia(b:String,sourceUrl:String) = runCatching { raw.downloadMedia(b,sourceUrl) }
    suspend fun uploadMedia(b:String,f:String,bytes:ByteArray,m:String) = decode(raw.uploadMedia(b,f,bytes,m)) { typedJson.decodeFromString<WooMediaDto>(it) }
    suspend fun deleteMedia(b:String,id:Long) = decode(raw.deleteMedia(b,id,true)) { Unit }
}

data class HttpApiException(val statusCode:Int,val body:String):Exception("WooCommerce HTTP $statusCode")
