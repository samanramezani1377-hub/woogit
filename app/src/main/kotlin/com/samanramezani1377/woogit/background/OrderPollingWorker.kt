package com.samanramezani1377.woogit.background

import android.content.Context
import androidx.work.*
import com.samanramezani1377.woogit.WooGitApplication
import java.util.concurrent.TimeUnit

class OrderPollingWorker(appContext:Context,params:WorkerParameters):CoroutineWorker(appContext,params){
 override suspend fun doWork():Result{
  val storeId=inputData.getString(KEY_STORE_ID)?:return Result.failure();val app=applicationContext as? WooGitApplication?:return Result.failure();val source=RepositoryOrderBackgroundSource(app.composition.getOrders);val store=OrderNotificationStore(applicationContext);val notifier=OrderNotificationManager(applicationContext)
  return runCatching{source.findNewOrders(storeId).forEach{order->val observed=store.lastObserved(order.storeId,order.orderId);val changed=observed==null||observed!=order.serverState;if(changed&&!store.wasNotified(order.storeId,order.orderId,order.serverState)){if(!notifier.notify(order))return Result.success() ;store.markNotified(order.storeId,order.orderId,order.serverState)};store.markObserved(order.storeId,order.orderId,order.serverState)};Result.success()}.getOrElse{Result.retry()}
 }
 companion object{private const val WORK_NAME="woogit-order-polling";const val KEY_STORE_ID="store_id";fun schedule(context:Context,storeId:String,repeatHours:Long=1L){val request=PeriodicWorkRequestBuilder<OrderPollingWorker>(repeatHours.coerceAtLeast(1L),TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setInputData(workDataOf(KEY_STORE_ID to storeId)).build();WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,ExistingPeriodicWorkPolicy.UPDATE,request)};fun cancel(context:Context){WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)}}
