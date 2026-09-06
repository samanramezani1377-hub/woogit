package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId

data class Product(
    val id: EntityId,
    val name: String,
    val sku: String?,
    val description: String?,
    val shortDescription: String?,
    val status: ProductStatus,
    val type: ProductType,
    val pricing: Pricing,
    val stock: Stock?,
    val images: List<ProductImage>,
    val categories: List<IdName>,
    val attributes: List<Attribute>,
    val modifiedAt: String?,
    val currency: String? = null,
    val currencySymbol: String? = null,
    val currencyPosition: String? = null,
    val thousandSeparator: String? = null,
    val decimalSeparator: String? = null,
    val numberOfDecimals: Int? = null,
    val weightUnit: String? = null,
    val dimensionUnit: String? = null,
    val slug: String? = null,
    val permalink: String? = null,
    val variationCount: Int? = null,
)
data class ProductImage(val id: EntityId?, val src: String, val name: String?, val alt: String?)
data class Pricing(val regular: String?, val sale: String?, val onSale: Boolean)
data class Stock(val quantity: Double?, val status: StockStatus, val manageStock: Boolean)
/** A catalog name can optionally carry its parent relation; existing callers remain source-compatible. */
data class IdName(val id: EntityId, val name: String, val parentId: EntityId? = null)
enum class ProductStatus { DRAFT, PUBLISHED, PENDING, PRIVATE, OTHER }
enum class ProductType { SIMPLE, GROUPED, EXTERNAL, VARIABLE, OTHER }
enum class StockStatus { IN_STOCK, OUT_OF_STOCK, ON_BACKORDER }
