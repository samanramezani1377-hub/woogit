package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.coroutines.CancellationException

class SyncEngine(private val db:WooGitDatabase,private val executor:OperationExecutor){
 suspend fun runOnce(now:Long){
  db.transaction{db.syncQueries.recoverRunning(now)}
  val operations=db.syncQueries.selectPending(now).executeAsList()
  operations.forEach{op->
   val claimed=db.transactionWithResult{db.syncQueries.claim(now,now,op.id);db.syncQueries.selectById(op.id).executeAsOneOrNull()?.state=="RUNNING"}
   if(!claimed)return@forEach
   try{executor.execute(op);db.transaction{db.syncQueries.updateState("SUCCEEDED",op.retry_count,null,null,now,op.id);db.syncQueries.upsertMetadata(op.store_id,"SUCCEEDED",null,null,now,now)}}
   catch(error:ConflictDetected){db.transaction{db.syncQueries.updateState("CONFLICT",op.retry_count,null,error.message,now,op.id);db.syncQueries.upsertMetadata(op.store_id,"CONFLICT",null,null,null,now)}}
   catch(error:CancellationException){throw error}
   catch(error:Throwable){val retryable=executor.isRetryable(error);val attempt=op.retry_count+1;val next=if(retryable)now+executor.backoffMillis(attempt)else null;val state=if(retryable&&attempt<executor.maxAttempts)"RETRYABLE_FAILURE"else"PERMANENT_FAILURE";db.transaction{db.syncQueries.updateState(state,attempt,next,error.message,now,op.id);db.syncQueries.upsertMetadata(op.store_id,state,null,null,null,now)}}
  }
 }
}
interface OperationExecutor{ suspend fun execute(operation:com.samanramezani1377.woogit.data.db.Pending_operation);fun isRetryable(error:Throwable):Boolean;fun backoffMillis(attempt:Int):Long=(1L shl attempt.coerceAtMost(10))*1000L;val maxAttempts:Int get()=5 }
