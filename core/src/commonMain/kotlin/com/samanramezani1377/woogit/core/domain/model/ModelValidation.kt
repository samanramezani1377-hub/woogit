package com.samanramezani1377.woogit.core.domain.model

fun StoreConnection.validate(): Boolean = baseUrl.startsWith("https://") && storeId.value.isNotBlank()
fun Product.validate(): Boolean = id.value.isNotBlank() && name.isNotBlank()
fun Order.validate(): Boolean = id.value.isNotBlank() && items.all { it.quantity >= 0.0 }
fun PendingOperation.validate(): Boolean = retryCount >= 0 && entityId.value.isNotBlank()
