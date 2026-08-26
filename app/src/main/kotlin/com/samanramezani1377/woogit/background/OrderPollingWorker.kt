package com.samanramezani1377.woogit.background

import android.content.Context
import androidx.work.*
import com.samanramezani1377.woogit.WooGitApplication
import java.io.IOException
import java.util.concurrent.TimeUnit

class OrderPollingWorker(appContext:Context,params:WorkerParameters):CoroutineWorker(appContext,params){
 override suspend fun doWork():Result{
  val storeId=inputData.getString(KEY_STORE_ID)?:return Result.failure();val app=applicationContext as? WooGitApplication?:return Result.failure()
  return try{val source=RepositoryOrderBackgroundSource(app.composition.getOrders);val store=OrderNotificationStore(applicationContext);val notifier=OrderNotificationManager(applicationContext);source.findNewOrders(storeId).forEach{order->if(!store.wasNotified(order.storeId,order.orderId,order.serverState)){if(!notifier.notify(order))return Result.retry();store.markNotified(order.storeId,order.orderId,order.serverState)}};Result.success()}catch(_:IOException){Result.retry()}catch(_:Exception){Result.retry()}
 }
 companion object{private const val WORK_NAME="woogit-order-polling";const val KEY_STORE_ID="store_id";fun schedule(context:Context,storeId:String,repeatHours:Long=1L){val request=PeriodicWorkRequestBuilder<OrderPollingWorker>(repeatHours.coerceAtLeast(1L),TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setInputData(workDataOf(KEY_STORE_ID to storeId)).build();WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,ExistingPeriodicWorkPolicy.KEEP,request)};fun cancel(context:Context){WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)}}
