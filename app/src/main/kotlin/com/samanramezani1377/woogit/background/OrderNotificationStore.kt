package com.samanramezani1377.woogit.background

import android.content.Context

/** Persistent notification state is intentionally separate from order observation state. */
class OrderNotificationStore(context:Context){
 private val prefs=context.getSharedPreferences("order_notifications",Context.MODE_PRIVATE)
 fun wasNotified(storeId:String,orderId:Long,serverState:String):Boolean=prefs.getString("notified.${key(storeId,orderId)}",null)==serverState
 fun markNotified(storeId:String,orderId:Long,serverState:String){prefs.edit().putString("notified.${key(storeId,orderId)}",serverState).apply()}
 fun lastObserved(storeId:String,orderId:Long):String?=prefs.getString("observed.${key(storeId,orderId)}",null)
 fun markObserved(storeId:String,orderId:Long,serverState:String){prefs.edit().putString("observed.${key(storeId,orderId)}",serverState).apply()}
 fun clear(storeId:String,orderId:Long){prefs.edit().remove("notified.${key(storeId,orderId)}").remove("observed.${key(storeId,orderId)}").apply()}
 private fun key(storeId:String,orderId:Long)="${storeId.trim()}:$orderId"
}
