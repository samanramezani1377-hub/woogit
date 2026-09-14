package com.samanramezani1377.woogit.domain.features.commerce

object CommerceFeatureRules {
    fun isLowStock(stockQuantity: Double?, threshold: Double): Boolean {
        return stockQuantity != null && stockQuantity >= 0.0 && stockQuantity <= threshold
    }

    fun isOutOfStock(stockQuantity: Double?, stockStatus: String): Boolean {
        return stockStatus.equals("outofstock", ignoreCase = true) || stockQuantity == 0.0
    }

    fun canBulkUpdateStatus(status: String): Boolean {
        return status in setOf(
            "pending",
            "processing",
            "on-hold",
            "completed",
            "cancelled",
            "refunded",
            "failed"
        )
    }

    fun normalizeBarcode(value: String): String {
        return value.trim()
    }
}
