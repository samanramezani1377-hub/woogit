package com.samanramezani1377.woogit.background

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.usecase.GetOrders

class RepositoryOrderBackgroundSource(private val getOrders:GetOrders):OrderBackgroundSource{
 override suspend fun findNewOrders(storeId:String):List<BackgroundOrder>{
  val result=getOrders(StoreId(storeId),1,20);if(result !is CoreResult.Success)return emptyList()
  return result.value.map{val version=it.modifiedAt?.toString()?:"${it.status.name}:${it.id.value}";val amount=it.items.sumOf{item->item.total.toDoubleOrNull()?:0.0};BackgroundOrder(storeId,it.id.value.toLong(),it.id.value,amount.toString(),"${it.items.size} items",it.modifiedAt?.toEpochMilliseconds()?:System.currentTimeMillis(),version)}
 }
}
