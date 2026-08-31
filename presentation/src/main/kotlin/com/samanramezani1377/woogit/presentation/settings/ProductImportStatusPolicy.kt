package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.model.ProductStatus

/** Centralizes the allowed fallback policy for imported products. */
object ProductImportStatusPolicy {
    fun initialStatus(requested: ProductStatus): ProductStatus = requested

    fun fallbackStatuses(requested: ProductStatus, allowUnexpectedPublish: Boolean): List<ProductStatus> = when (requested) {
        ProductStatus.DRAFT -> buildList {
            add(ProductStatus.PENDING)
            if (allowUnexpectedPublish) add(ProductStatus.PUBLISHED)
        }
        ProductStatus.PENDING -> buildList {
            if (allowUnexpectedPublish) add(ProductStatus.PUBLISHED)
        }
        else -> emptyList()
    }

    fun isAllowedFinalStatus(requested: ProductStatus, actual: ProductStatus, allowUnexpectedPublish: Boolean): Boolean =
        actual == requested || (allowUnexpectedPublish && actual == ProductStatus.PUBLISHED)
}
