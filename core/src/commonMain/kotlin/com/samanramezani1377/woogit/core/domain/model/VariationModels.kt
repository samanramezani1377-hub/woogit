package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import kotlinx.datetime.Instant

data class Variation(
    val id: EntityId,
    val productId: EntityId,
    val attributes: List<VariationAttribute>,
    val pricing: Pricing,
    val stock: Stock?,
    val sku: String?,
    val image: ProductImage?,
    val modifiedAt: Instant? = null,
)

data class VariationAttribute(val name: String, val option: String)
