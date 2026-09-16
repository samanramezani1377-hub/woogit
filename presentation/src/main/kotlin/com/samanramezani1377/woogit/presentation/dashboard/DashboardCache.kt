package com.samanramezani1377.woogit.presentation.dashboard

import android.content.Context

internal data class DashboardCachedMetrics(
    val ordersTotal: Int?,
    val processingTotal: Int?,
    val productsTotal: Int?,
    val revenue: String?,
)

internal class DashboardCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("woogit_dashboard_cache", Context.MODE_PRIVATE)

    fun read(storeId: String): DashboardCachedMetrics = DashboardCachedMetrics(
        ordersTotal = readInt(storeId, "orders_total"),
        processingTotal = readInt(storeId, "processing_total"),
        productsTotal = readInt(storeId, "products_total"),
        revenue = preferences.getString(key(storeId, "revenue"), null),
    )

    fun write(storeId: String, metrics: DashboardCachedMetrics) {
        preferences.edit().apply {
            putNullableInt(key(storeId, "orders_total"), metrics.ordersTotal)
            putNullableInt(key(storeId, "processing_total"), metrics.processingTotal)
            putNullableInt(key(storeId, "products_total"), metrics.productsTotal)
            if (metrics.revenue != null) putString(key(storeId, "revenue"), metrics.revenue) else remove(key(storeId, "revenue"))
        }.apply()
    }

    private fun readInt(storeId: String, name: String): Int? = if (preferences.contains(key(storeId, name))) preferences.getInt(key(storeId, name), 0) else null
    private fun key(storeId: String, name: String) = "$storeId:$name"
    private fun android.content.SharedPreferences.Editor.putNullableInt(key: String, value: Int?) = if (value == null) remove(key) else putInt(key, value)
}
