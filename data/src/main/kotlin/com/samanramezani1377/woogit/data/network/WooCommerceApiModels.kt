package com.samanramezani1377.woogit.data.network

import kotlinx.serialization.Serializable

@Serializable
data class WooSystemStatusSettingsDto(
    val currency: String = "",
    val currency_symbol: String = "",
    val currency_position: String = "left",
    val thousand_separator: String = ",",
    val decimal_separator: String = ".",
    val number_of_decimals: Int = 2,
)

@Serializable
data class WooOrderDto(val id: Long, val status: String = "", val total: String = "", val currency: String = "")

@Serializable
data class WooProductDto(val id: Long, val name: String = "", val type: String = "", val status: String = "", val price: String? = null)

@Serializable
data class WooVariationDto(val id: Long, val sku: String? = null, val price: String? = null)

@Serializable
data class WooAttributeDto(val id: Long? = null, val name: String = "", val slug: String = "")

@Serializable
data class WooImageDto(val id: Long? = null, val src: String = "", val name: String = "", val alt: String = "")

@Serializable
data class WooSalesReportDto(
    val total_sales: String = "0",
    val net_sales: String = "0",
    val total_orders: Int = 0,
    val total_items: Int = 0,
)
