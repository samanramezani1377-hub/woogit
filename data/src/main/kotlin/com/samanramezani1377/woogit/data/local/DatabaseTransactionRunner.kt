package com.samanramezani1377.woogit.data.local

import app.cash.sqldelight.db.SqlDriver

class DatabaseTransactionRunner(private val driver: SqlDriver) {
    fun <T> transaction(block: () -> T): T {
        val transaction = driver.newTransaction().value
        return try {
            val result = block()
            transaction.endTransaction(successful = true)
            result
        } catch (t: Throwable) {
            transaction.endTransaction(successful = false)
            throw t
        }
    }
}
