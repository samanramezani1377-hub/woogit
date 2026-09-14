package com.samanramezani1377.woogit.presentation.commerce

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Product

object InventoryRuntime {
    var updateProduct: (suspend (StoreId, Product) -> CoreResult<Product>)? = null
}
