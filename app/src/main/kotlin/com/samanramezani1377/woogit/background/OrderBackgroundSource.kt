package com.samanramezani1377.woogit.background

/**
 * Background-safe source used by WorkManager. Implementations must use the existing
 * repository/network boundary and must not retain a permanent connection.
 */
interface OrderBackgroundSource {
    suspend fun findNewOrders(storeId: String): List<BackgroundOrder>
}

data class BackgroundOrder(
    val storeId: String,
    val orderId: Long,
    val number: String,
    val total: String,
    val itemSummary: String,
    val occurredAtEpochMillis: Long,
)

object OrderBackgroundSourceRegistry {
    @Volatile
    var source: OrderBackgroundSource? = null
}
