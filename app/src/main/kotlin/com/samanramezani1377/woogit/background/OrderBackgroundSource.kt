package com.samanramezani1377.woogit.background

interface OrderBackgroundSource { suspend fun findNewOrders(storeId: String): List<BackgroundOrder> }

data class BackgroundOrder(
    val storeId: String,
    val orderId: Long,
    val number: String,
    val total: String,
    val currency: String,
    val itemSummary: String,
    val occurredAtEpochMillis: Long,
    val serverState: String,
)
