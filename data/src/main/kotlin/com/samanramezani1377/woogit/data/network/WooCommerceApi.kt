package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.CredentialPair
import io.ktor.client.HttpClient

/** Compatibility facade: every Customer API operation is now transported through WooGit Backend /forward. */
class WooCommerceApi(
    private val backend: BackendClient,
    private val storeId: String,
    private val credentials: CredentialPair,
) {
    suspend fun validateStore(baseUrl: String) = get("/wp-json/wc/v3/system_status")
    suspend fun listOrders(b:String,page:Int=1,perPage:Int=20,search:String?=null,status:String?=null)=get("/wp-json/wc/v3/orders", q(page,perPage,search,status))
    suspend fun getOrder(b:String,id:Long)=get("/wp-json/wc/v3/orders/$id")
    suspend fun updateOrder(b:String,id:Long,body:String)=mut("/wp-json/wc/v3/orders/$id","PUT",body)
    suspend fun listOrderNotes(b:String,id:Long,page:Int=1,perPage:Int=100)=get("/wp-json/wc/v3/orders/$id/notes",q(page,perPage))
    suspend fun addOrderNote(b:String,id:Long,body:String)=mut("/wp-json/wc/v3/orders/$id/notes","POST",body)
    suspend fun deleteOrder(b:String,id:Long,force:Boolean=false)=mut("/wp-json/wc/v3/orders/$id","DELETE",null,mapOf("force" to force))
    suspend fun salesReport(b:String,dateMin:String,dateMax:String)=get("/wp-json/wc/v3/reports/sales",mapOf("date_min" to dateMin,"date_max" to dateMax))
    suspend fun listProducts(b:String,page:Int=1,perPage:Int=20,search:String?=null,modifiedAfter:String?=null)=get("/wp-json/wc/v3/products",q(page,perPage,search,null,modifiedAfter))
    suspend fun getProduct(b:String,id:Long)=get("/wp-json/wc/v3/products/$id")
    suspend fun createProduct(b:String,body:String)=mut("/wp-json/wc/v3/products","POST",body)
    suspend fun updateProduct(b:String,id:Long,body:String)=mut("/wp-json/wc/v3/products/$id","PUT",body)
    suspend fun deleteProduct(b:String,id:Long,force:Boolean=false)=mut("/wp-json/wc/v3/products/$id","DELETE",null,mapOf("force" to force))
    suspend fun listProductCategories(b:String,page:Int=1,perPage:Int=100,search:String?=null)=get("/wp-json/wc/v3/products/categories",q(page,perPage,search))
    suspend fun createProductCategory(b:String,body:String)=mut("/wp-json/wc/v3/products/categories","POST",body)
    suspend fun listVariations(b:String,productId:Long,page:Int=1,perPage:Int=20)=get("/wp-json/wc/v3/products/$productId/variations",q(page,perPage))
    suspend fun getVariation(b:String,productId:Long,id:Long)=get("/wp-json/wc/v3/products/$productId/variations/$id")
    suspend fun createVariation(b:String,productId:Long,body:String)=mut("/wp-json/wc/v3/products/$productId/variations","POST",body)
    suspend fun updateVariation(b:String,productId:Long,id:Long,body:String)=mut("/wp-json/wc/v3/products/$productId/variations/$id","PUT",body)
    suspend fun deleteVariation(b:String,productId:Long,id:Long,force:Boolean=false)=mut("/wp-json/wc/v3/products/$productId/variations/$id","DELETE",null,mapOf("force" to force))
    suspend fun listAttributes(b:String,page:Int=1,perPage:Int=100)=get("/wp-json/wc/v3/products/attributes",q(page,perPage))
    suspend fun getAttribute(b:String,id:Long)=get("/wp-json/wc/v3/products/attributes/$id")
    suspend fun createAttribute(b:String,body:String)=mut("/wp-json/wc/v3/products/attributes","POST",body)
    suspend fun updateAttribute(b:String,id:Long,body:String)=mut("/wp-json/wc/v3/products/attributes/$id","PUT",body)
    suspend fun deleteAttribute(b:String,id:Long,force:Boolean=false)=mut("/wp-json/wc/v3/products/attributes/$id","DELETE",null,mapOf("force" to force))
    suspend fun listAttributeTerms(b:String,attributeId:Long,page:Int=1,perPage:Int=100)=get("/wp-json/wc/v3/products/attributes/$attributeId/terms",q(page,perPage))
    suspend fun getAttributeTerm(b:String,attributeId:Long,id:Long)=get("/wp-json/wc/v3/products/attributes/$attributeId/terms/$id")
    suspend fun createAttributeTerm(b:String,attributeId:Long,body:String)=mut("/wp-json/wc/v3/products/attributes/$attributeId/terms","POST",body)
    suspend fun updateAttributeTerm(b:String,attributeId:Long,id:Long,body:String)=mut("/wp-json/wc/v3/products/attributes/$attributeId/terms/$id","PUT",body)
    suspend fun deleteAttributeTerm(b:String,attributeId:Long,id:Long,force:Boolean=false)=mut("/wp-json/wc/v3/products/attributes/$attributeId/terms/$id","DELETE",null,mapOf("force" to force))
    suspend fun listMedia(b:String,page:Int=1,perPage:Int=30,search:String?=null)=get("/wp-json/wp/v2/media",q(page,perPage,search))
    suspend fun uploadMedia(b:String,fileName:String,bytes:ByteArray,mediaType:String)=backend.forwardBinary(storeId,"/wp-json/wp/v2/media","POST",credentials,bytes,mediaType,fileName)
    suspend fun deleteMedia(b:String,mediaId:Long,force:Boolean=true)=mut("/wp-json/wp/v2/media/$mediaId","DELETE",null,mapOf("force" to force))
    suspend fun downloadMedia(b:String,sourceUrl:String):ByteArray = error("Direct image download is owned by ImageFetcher, not WooCommerceApi")

    private suspend fun get(path:String,query:Map<String,Any> = emptyMap())=backend.forward(storeId,path,"GET",credentials,query)
    private suspend fun mut(path:String,method:String,body:String?,query:Map<String,Any> = emptyMap())=backend.forward(storeId,path,method,credentials,query,body)
    private fun q(page:Int,perPage:Int,search:String?=null,status:String?=null,modifiedAfter:String?=null)=buildMap<String,Any>{put("page",page);put("per_page",perPage);if(!search.isNullOrBlank())put("search",search);if(!status.isNullOrBlank())put("status",status);if(!modifiedAfter.isNullOrBlank()){put("modified_after",modifiedAfter);put("dates_are_gmt",true)}}
}

data class ApiResponse(val statusCode:Int,val body:String,val method:String="",val endpoint:String="",val headers:Map<String,String> = emptyMap()) { val total:Int? get()=headers["x-wp-total"]?.toIntOrNull(); val totalPages:Int? get()=headers["x-wp-totalpages"]?.toIntOrNull() }
