package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.data.db.WooGitDatabase

class LocalStoreRepository(private val db: WooGitDatabase) {
    fun find(id: String) = db.storeQueries.selectById(id).executeAsOneOrNull()
    fun list() = db.storeQueries.selectAll().executeAsList()
}

class LocalOrderRepository(private val db: WooGitDatabase) {
    fun find(id: String) = db.orderQueries.selectById(id).executeAsOneOrNull()
    fun list(storeId: String) = db.orderQueries.selectByStore(storeId).executeAsList()
}

class LocalProductRepository(private val db: WooGitDatabase) {
    fun find(id: String) = db.productQueries.selectById(id).executeAsOneOrNull()
    fun list(storeId: String) = db.productQueries.selectByStore(storeId).executeAsList()
}

class LocalSyncQueueRepository(private val db: WooGitDatabase) {
    fun pending(now: Long) = db.syncQueries.selectPending(now).executeAsList()
}
