package com.samanramezani1377.woogit.background

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.usecase.GetOrders

class RepositoryOrderBackgroundSource(private val getOrders:GetOrders):OrderBackgroundSource{
 override suspend fun findNewOrders(storeId:String):List<BackgroundOrder>{
  val observedStore=OrderNotificationStoreHolder.current ?: return fetchAll(storeId)
  val result=buildList{
   var page=1
   while(true){
    val response=getOrders(StoreId(storeId),page,50)
    if(response !is CoreResult.Success) break
    val orders=response.value
    if(orders.isEmpty()) break
    var stable=true
    orders.forEach{order->
      val version=order.modifiedAt?.toString()?:"${order.status.name}:${order.id.value}"
      val observed=observedStore.lastObserved(storeId,order.id.value.toLong())
      if(observed!=version) stable=false
      add(BackgroundOrder(storeId,order.id.value.toLong(),order.number,order.total?:order.items.sumOf{it.total.toDoubleOrNull()?:0.0}.toString(),"${order.items.size} items",order.modifiedAt?.toEpochMilliseconds()?:System.currentTimeMillis(),version))
    }
    page++
    // WooCommerce returns orders newest-first. Once an entire page is already observed,
    // older pages cannot contain a newer order in the normal polling path.
    if(stable || orders.size<50) break
   }
  }
  return result.distinctBy{it.orderId}
 }
 private suspend fun fetchAll(storeId:String):List<BackgroundOrder>{
  val result=buildList{var page=1;do{val response=getOrders(StoreId(storeId),page,50);if(response !is CoreResult.Success)break;val orders=response.value;addAll(orders.map{order->val version=order.modifiedAt?.toString()?:"${order.status.name}:${order.id.value}";BackgroundOrder(storeId,order.id.value.toLong(),order.number,order.total?:order.items.sumOf{it.total.toDoubleOrNull()?:0.0}.toString(),"${order.items.size} items",order.modifiedAt?.toEpochMilliseconds()?:System.currentTimeMillis(),version)});page++;}while(orders.size==50)}
  return result.distinctBy{it.orderId}
 }
}

internal object OrderNotificationStoreHolder { var current: OrderNotificationStore? = null }
