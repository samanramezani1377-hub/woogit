package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.model.ProductImage
import kotlinx.serialization.Serializable

@Serializable
data class ProductTransferManifest(
    val format: String,
    val version: Int,
    val source: String,
    val exportedAt: String,
    val products: Int,
    val images: Int,
)

@Serializable
data class ProductTransferPackage(
    val manifest: ProductTransferManifest,
    val products: List<TransferProduct>,
)

@Serializable
data class TransferProduct(
    val id: String,
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val status: String,
    val type: String,
    val regular: String? = null,
    val sale: String? = null,
    val onSale: Boolean,
    val quantity: Double? = null,
    val stockStatus: String? = null,
    val manageStock: Boolean,
    val categories: List<TransferIdName> = emptyList(),
    val attributes: List<TransferAttribute> = emptyList(),
    val images: List<TransferImage> = emptyList(),
    val variations: List<TransferVariation> = emptyList(),
    val modifiedAt: String? = null,
)

@Serializable
data class TransferVariation(
    val id: String,
    val sku: String? = null,
    val regular: String? = null,
    val sale: String? = null,
    val onSale: Boolean,
    val quantity: Double? = null,
    val stockStatus: String? = null,
    val manageStock: Boolean,
    val attributes: List<TransferVariationAttribute> = emptyList(),
    val image: TransferImage? = null,
    val modifiedAt: String? = null,
)

@Serializable
data class TransferVariationAttribute(val name: String, val option: String)

@Serializable
data class TransferIdName(val id: String, val name: String, val parentId: String? = null)

@Serializable
data class TransferAttribute(
    val id: String? = null,
    val name: String,
    val visible: Boolean,
    val variation: Boolean,
    val options: List<String>,
)

@Serializable
data class TransferImage(
    val id: String? = null,
    val src: String,
    val name: String? = null,
    val alt: String? = null,
    val file: String,
)

data class TransferMedia(val file: String, val image: ProductImage)
