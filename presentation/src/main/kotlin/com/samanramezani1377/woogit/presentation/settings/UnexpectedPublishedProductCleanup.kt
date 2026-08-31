package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

/** Removes only products created by the current strict draft import that ended up published. */
internal class UnexpectedPublishedProductCleanup(private val d: V1PresentationDependencies) {
    private val reader = ProductTransferRepositoryReader(d, 100)

    suspend fun cleanup(storeId: StoreId, existingIds: Set<String>): Int {
        val current = reader.products(storeId) { }
        var removed = 0
        current.asSequence()
            .filter { it.id.value !in existingIds && it.status == ProductStatus.PUBLISHED }
            .forEach { product ->
                val deleted = d.deleteProduct(storeId, EntityId(product.id.value)) is CoreResult.Success
                if (deleted) {
                    removed++
                } else {
                    // If WooCommerce refuses deletion, immediately force the product back
                    // to a non-public state. This is the final safety net for strict draft import.
                    when (d.updateProduct(storeId, product.id, product.copy(status = ProductStatus.DRAFT))) {
                        is CoreResult.Success -> removed++
                        is CoreResult.Failure -> Unit
                    }
                }
            }
        return removed
    }
}
