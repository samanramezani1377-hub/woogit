package com.samanramezani1377.woogit.presentation.customers

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Customer

/** Presentation bridge for the local-first customer repository. */
object CustomerRuntime {
    var listLoader: (suspend (StoreId, Int, Int, String?) -> CoreResult<List<Customer>>)? = null
    var detailLoader: (suspend (StoreId, EntityId) -> CoreResult<Customer>)? = null
}
