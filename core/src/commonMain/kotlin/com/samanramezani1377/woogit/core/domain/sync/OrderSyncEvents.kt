package com.samanramezani1377.woogit.core.domain.sync

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.Order
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class OrderSyncUpdate(
    val storeId: StoreId,
    val orders: List<Order>,
)

object OrderSyncEvents {
    private val _updates = MutableSharedFlow<OrderSyncUpdate>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    val updates: SharedFlow<OrderSyncUpdate> = _updates.asSharedFlow()

    fun publish(update: OrderSyncUpdate) {
        _updates.tryEmit(update)
    }
}
