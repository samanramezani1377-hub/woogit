package com.samanramezani1377.woogit.core.domain.sync

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.Product
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ProductSyncUpdate(
    val storeId: StoreId,
    val products: List<Product>,
)

object ProductSyncEvents {
    // A sync may finish while a screen is being recreated. Keep the latest
    // completed update so a new collector can reconcile its state from it.
    private val _updates = MutableSharedFlow<ProductSyncUpdate>(replay = 1, extraBufferCapacity = 16)
    val updates: SharedFlow<ProductSyncUpdate> = _updates.asSharedFlow()

    fun publish(update: ProductSyncUpdate) {
        _updates.tryEmit(update)
    }
}
