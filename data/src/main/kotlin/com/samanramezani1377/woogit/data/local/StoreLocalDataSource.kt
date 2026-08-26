package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.data.db.WooGitDatabase

class StoreLocalDataSource(private val database: WooGitDatabase) {
    private val queries get() = database.storeQueries

    fun find(id: String) = queries.selectById(id).executeAsOneOrNull()

    fun all() = queries.selectAll().executeAsList()

    fun upsert(id: String, baseUrl: String, credentialReference: String?, state: String, now: Long) =
        queries.insert(id, baseUrl, credentialReference, state, now, now)

    fun delete(id: String) = queries.deleteById(id)
}
