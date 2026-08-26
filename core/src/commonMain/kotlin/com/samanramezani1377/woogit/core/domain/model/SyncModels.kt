package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.EntityTimestamp
import com.samanramezani1377.woogit.core.domain.entity.StoreId

enum class SyncState { IDLE, RUNNING, SUCCEEDED, FAILED, CONFLICT }
enum class OperationType { CREATE, UPDATE, DELETE }
enum class ConflictResolution { KEEP_LOCAL, KEEP_SERVER, MERGE }

data class PendingOperation(val id: EntityId,val storeId: StoreId,val entityType: String,val entityId: EntityId,val type: OperationType,val payloadJson: String,val payloadHash: String,val retryCount: Int,val lastAttemptAt: EntityTimestamp?,val nextAttemptAt: EntityTimestamp? = null)
data class EntityVersion(val value: String,val modifiedAt: EntityTimestamp?)
data class Conflict(val id: EntityId,val entityId: EntityId,val localVersion: EntityVersion?,val remoteVersion: EntityVersion?,val reason: String,val localSnapshot:String?=null,val serverSnapshot:String?=null,val resolution: ConflictResolution? = null)
data class SyncMetadata(val state: SyncState,val version: EntityVersion?,val pending: List<PendingOperation>)
