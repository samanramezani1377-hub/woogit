package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.PendingOperationRepository
import com.samanramezani1377.woogit.core.domain.repository.SyncRepository
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.datetime.Instant

class SyncRepositoryImpl(private val db:WooGitDatabase,private val engine:SyncEngine,private val pending:PendingOperationRepository):SyncRepository{
 override suspend fun sync(storeId:StoreId):CoreResult<Unit>{engine.runOnce(System.currentTimeMillis());return CoreResult.Success(Unit)}
 override suspend fun getState(storeId:StoreId):CoreResult<SyncMetadata>{val row=db.syncQueries.selectMetadata(storeId.value).executeAsOneOrNull();val queue=pending.getPending(storeId);val state=row?.state?.let{runCatching{SyncState.valueOf(it)}.getOrDefault(SyncState.IDLE)}?:SyncState.IDLE;val version=row?.version?.let{EntityVersion(it,row.modified_at?.let(Instant::parse))};return if(queue is CoreResult.Success)CoreResult.Success(SyncMetadata(state,version,queue.value)) else CoreResult.Failure((queue as CoreResult.Failure).error)}
}
