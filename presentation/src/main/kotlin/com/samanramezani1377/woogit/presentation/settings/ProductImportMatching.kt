package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.Variation

internal object ProductImportMatching {
    fun findProduct(
        x: TransferProduct,
        sameStore: Boolean,
        byId: Map<String, Product>,
        bySku: Map<String, Product>,
        byFingerprint: Map<String, List<Product>>
    ): Product? {
        if (sameStore) byId[x.id]?.let { return it }
        cleanSku(x.sku)?.let { sku -> bySku[normalize(sku)]?.let { return it } }
        return byFingerprint[x.matchKey()].orEmpty().singleOrNull()
    }

    fun findVariation(existing: List<Variation>, source: TransferVariation): Variation? {
        cleanSku(source.sku)?.let { sku ->
            existing.firstOrNull { cleanSku(it.sku)?.let(::normalize) == normalize(sku) }
                ?.let { return it }
        }
        val key = source.attributeKey()
        return existing.singleOrNull { it.attributeKey() == key }
    }
}
