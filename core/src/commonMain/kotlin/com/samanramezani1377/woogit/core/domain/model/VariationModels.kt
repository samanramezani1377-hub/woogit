package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId

data class Variation(val id: EntityId, val productId: EntityId, val attributes: List<VariationAttribute>, val pricing: Pricing, val stock: Stock?, val sku: String?, val image: ProductImage?)
data class VariationAttribute(val name: String, val option: String)
