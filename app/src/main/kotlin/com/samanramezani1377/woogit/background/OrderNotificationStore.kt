package com.samanramezani1377.woogit.background

import android.content.Context

class OrderNotificationStore(context:Context){
 private val prefs=context.getSharedPreferences("order_notifications",Context.MODE_PRIVATE)
 fun wasNotified(storeId:String,orderId:Long,serverState:String):Boolean=prefs.getString(key(storeId,orderId),null)==serverState
 fun markNotified(storeId:String,orderId:Long,serverState:String){prefs.edit().putString(key(storeId,orderId),serverState).apply()}
 fun clear(storeId:String,orderId:Long){prefs.edit().remove(key(storeId,orderId)).apply()}
 private fun key(storeId:String,orderId:Long)="${storeId.trim()}:$orderId"
}
