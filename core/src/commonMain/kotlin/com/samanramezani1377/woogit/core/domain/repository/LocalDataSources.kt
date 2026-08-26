package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*

interface LocalOrderDataSource<T> { fun get(storeId:StoreId,id:EntityId):CoreResult<T>; fun list(storeId:StoreId):CoreResult<List<T>>; fun upsert(storeId:StoreId,value:T):CoreResult<Unit>; fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit> }
interface LocalProductDataSource<T> { fun get(storeId:StoreId,id:EntityId):CoreResult<T>; fun list(storeId:StoreId):CoreResult<List<T>>; fun upsert(storeId:StoreId,value:T):CoreResult<Unit>; fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit> }
interface LocalStoreDataSource<T> { fun get(storeId:StoreId):CoreResult<T>; fun upsert(value:T):CoreResult<Unit>; fun delete(storeId:StoreId):CoreResult<Unit> }
interface LocalVariationDataSource { fun list(storeId:StoreId,productId:EntityId):CoreResult<List<Variation>>; fun get(storeId:StoreId,productId:EntityId,id:EntityId):CoreResult<Variation>; fun upsert(storeId:StoreId,value:Variation):CoreResult<Unit>; fun delete(storeId:StoreId,productId:EntityId,id:EntityId):CoreResult<Unit> }
interface LocalAttributeDataSource { fun list(storeId:StoreId):CoreResult<List<GlobalAttribute>>; fun get(storeId:StoreId,id:EntityId):CoreResult<GlobalAttribute>; fun upsert(storeId:StoreId,value:GlobalAttribute):CoreResult<Unit>; fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit> }
interface LocalTermDataSource { fun list(storeId:StoreId,attributeId:EntityId):CoreResult<List<AttributeTerm>>; fun upsert(storeId:StoreId,attributeId:EntityId,value:AttributeTerm):CoreResult<Unit>; fun delete(storeId:StoreId,attributeId:EntityId,id:EntityId):CoreResult<Unit> }
interface MutationCoordinator { fun <T> execute(operation:PendingOperation,localMutation:()->CoreResult<T>):CoreResult<T> }
