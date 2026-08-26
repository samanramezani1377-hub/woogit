package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class SyncRepositoryImpl(private val db:WooGitDatabase,private val engine:SyncEngine,private val pending:PendingOperationRepository):SyncRepository{
 override suspend fun sync(storeId:StoreId):CoreResult<Unit>{engine.runOnce(System.currentTimeMillis());return CoreResult.Success(Unit)}
 override suspend fun getState(storeId:StoreId):CoreResult<SyncMetadata>{val row=db.syncQueries.selectMetadata(storeId.value).executeAsOneOrNull();val queue=pending.getPending(storeId);val state=row?.state?.let{runCatching{SyncState.valueOf(it)}.getOrDefault(SyncState.IDLE)}?:SyncState.IDLE;val version=row?.version?.let{EntityVersion(it,row.modified_at?.let(Instant::parse))};return if(queue is CoreResult.Success)CoreResult.Success(SyncMetadata(state,version,queue.value))else CoreResult.Failure((queue as CoreResult.Failure).error)}
 override suspend fun getConflicts(storeId:StoreId):CoreResult<List<Conflict>>=CoreResult.Success(db.syncQueries.selectConflicts(storeId.value).executeAsList().map{Conflict(EntityId(it.id),EntityId(it.entity_id),EntityVersion("local",null),EntityVersion("server",null),it.conflicting_fields,null)})
 override suspend fun resolveConflict(storeId:StoreId,conflictId:EntityId,resolution:ConflictResolution):CoreResult<Unit>{val row=db.syncQueries.selectConflictById(conflictId.value).executeAsOneOrNull()?:return CoreResult.Failure(DomainError.NotFound("conflict",conflictId.value));if(row.store_id!=storeId.value)return CoreResult.Failure(DomainError.NotFound("conflict",conflictId.value));return when(resolution){ConflictResolution.KEEP_LOCAL->{db.transaction{db.syncQueries.resolveConflict("RESOLVED_LOCAL",System.currentTimeMillis(),conflictId.value,storeId.value);db.syncQueries.updateState("PENDING",0,null,null,null,System.currentTimeMillis(),row.operation_id)};CoreResult.Success(Unit)};ConflictResolution.KEEP_SERVER->{val json=Json{ignoreUnknownKeys=true};when(row.entity_type){"order"->{val dto=json.decodeFromString<com.samanramezani1377.woogit.data.network.WooOrderTypedDto>(row.server_snapshot);com.samanramezani1377.woogit.data.local.SqlOrderDataSource(db).upsert(storeId,com.samanramezani1377.woogit.data.repository.OrderRepositoryV1Mapper.toDomain(dto))}"product"->{val dto=json.decodeFromString<com.samanramezani1377.woogit.data.network.WooProductTypedDto>(row.server_snapshot);com.samanramezani1377.woogit.data.local.SqlProductDataSource(db).upsert(storeId,com.samanramezani1377.woogit.data.repository.ProductRepositoryV1Mapper.toDomain(dto))}else->return CoreResult.Failure(DomainError.Validation("Unsupported conflict entity"))};db.transaction{db.syncQueries.resolveConflict("RESOLVED_SERVER",System.currentTimeMillis(),conflictId.value,storeId.value);db.syncQueries.updateState("SUCCEEDED",0,null,null,null,System.currentTimeMillis(),row.operation_id)};CoreResult.Success(Unit)};ConflictResolution.MERGE->CoreResult.Failure(DomainError.Validation("Automatic merge is not safe for this V1 resource"))}}
}
