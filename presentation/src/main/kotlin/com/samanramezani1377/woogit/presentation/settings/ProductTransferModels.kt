package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.model.ProductImage
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable data class ProductTransferManifest(@EncodeDefault(EncodeDefault.Mode.ALWAYS) val format:String=ProductTransferFormat.FORMAT,@EncodeDefault(EncodeDefault.Mode.ALWAYS) val version:Int=ProductTransferFormat.FORMAT_VERSION,@EncodeDefault(EncodeDefault.Mode.ALWAYS) val layoutVersion:Int=ProductTransferFormat.LAYOUT_VERSION,val source:String,val exportedAt:String,val products:Int,val images:Int)
@Serializable data class ProductTransferPackage(val manifest:ProductTransferManifest,val products:List<TransferProduct>,val globalAttributes:List<TransferGlobalAttribute> = emptyList())
@Serializable data class TransferGlobalAttribute(val id:String,val name:String,val slug:String,val terms:List<TransferTerm> = emptyList())
@Serializable data class TransferTerm(val id:String?=null,val name:String,val slug:String?=null)
@Serializable data class TransferProduct(val id:String,val name:String,val sku:String?=null,val description:String?=null,val shortDescription:String?=null,val status:String,val type:String,val regular:String?=null,val sale:String?=null,val onSale:Boolean,val quantity:Double?=null,val stockStatus:String?=null,val manageStock:Boolean,val categories:List<TransferCategory> = emptyList(),val attributes:List<TransferAttribute> = emptyList(),val images:List<TransferImage> = emptyList(),val variations:List<TransferVariation> = emptyList(),val modifiedAt:String?=null)
@Serializable data class TransferVariation(val id:String,val sku:String?=null,val regular:String?=null,val sale:String?=null,val onSale:Boolean,val quantity:Double?=null,val stockStatus:String?=null,val manageStock:Boolean,val attributes:List<TransferVariationAttribute> = emptyList(),val image:TransferImage?=null,val modifiedAt:String?=null)
@Serializable data class TransferVariationAttribute(val name:String,val option:String)
@Serializable data class TransferCategory(val id:String,val name:String,val parentId:String?=null)
@Serializable data class TransferAttribute(val id:String?=null,val name:String,val visible:Boolean,val variation:Boolean,val options:List<String>)
@Serializable data class TransferImage(val id:String?=null,val src:String,val name:String?=null,val alt:String?=null,val file:String)
data class TransferValidatedPackage(val manifest:ProductTransferManifest,val products:List<TransferProduct>,val mediaNames:Set<String>,val invalidProductIds:Set<String>,val validationErrors:List<String>,val globalAttributes:List<TransferGlobalAttribute> = emptyList())
data class TransferMediaOutcome(val images:Map<String,ProductImage>,val failed:Int,val errors:List<String>,val uploaded:Int,val reused:Int=0)
data class RobustProductTransferResult(val created:Int=0,val updated:Int=0,val failed:Int=0,val imagesUploaded:Int=0,val variationsCreated:Int=0,val variationsUpdated:Int=0,val errors:List<String> = emptyList(),val variationsFailed:Int=0,val imagesFailed:Int=0,val imagesUnused:Int=0,val skuChanged:Int=0,val validationErrors:List<String> = emptyList(),val importErrors:List<String> = emptyList(),val drafted:Int=0,val categoriesCreated:Int=0,val categoriesResolved:Int=0,val attributesCreated:Int=0,val attributesResolved:Int=0,val termsCreated:Int=0,val termsResolved:Int=0,val imagesReused:Int=0,val publishedUnexpectedly:Int=0)