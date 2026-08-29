package com.samanramezani1377.woogit.background

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.usecase.GetOrders

class RepositoryOrderBackgroundSource(private val getOrders: GetOrders, private val observedStore: OrderNotificationStore) : OrderBackgroundSource {
    override suspend fun findNewOrders(storeId: String): List<BackgroundOrder> {
        val result = buildList {
            var page = 1
            while (true) {
                val response = getOrders(StoreId(storeId), page, 50)
                if (response !is CoreResult.Success) break
                val orders = response.value
                if (orders.isEmpty()) break
                var stable = true
                orders.forEach { order ->
                    val version = order.modifiedAt?.toString() ?: "${order.status.name}:${order.id.value}"
                    val observed = observedStore.lastObserved(storeId, order.id.value.toLong())
                    if (observed != version) stable = false
                    add(
                        BackgroundOrder(
                            storeId = storeId,
                            orderId = order.id.value.toLong(),
                            number = order.number,
                            total = order.total ?: order.items.sumOf { it.total.toDoubleOrNull() ?: 0.0 }.toString(),
                            currency = order.currency.orEmpty(),
                            itemSummary = "${order.items.size} کالا",
                            occurredAtEpochMillis = order.modifiedAt?.toEpochMilliseconds() ?: System.currentTimeMillis(),
                            serverState = version,
                        )
                    )
                }
                page++
                if (stable || orders.size < 50) break
            }
        }
        return result.distinctBy { it.orderId }
    }
}
