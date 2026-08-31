package com.samanramezani1377.woogit.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

/**
 * Backward-compatible facade for product transfer.
 * The implementation is split into focused import/export services.
 */
class RobustProductTransferService(
    d: V1PresentationDependencies,
    resolver: ContentResolver
) {
    private val exportService = ProductTransferExportService(d, resolver)
    private val importService = ProductTransferImportService(d, resolver)

    suspend fun export(
        storeId: StoreId,
        destination: Uri,
        onProgress: (ProductTransferProgress) -> Unit = {}
    ): Result<Int> = exportService.export(storeId, destination, onProgress)

    suspend fun import(
        storeId: StoreId,
        source: Uri,
        mode: ProductImportMode = ProductImportMode.UPDATE_EXISTING,
        options: ProductImportOptions = ProductImportOptions(),
        onProgress: (ProductTransferProgress) -> Unit = {}
    ): RobustProductTransferResult = importService.import(
        storeId,
        source,
        mode,
        options,
        onProgress
    )
}
