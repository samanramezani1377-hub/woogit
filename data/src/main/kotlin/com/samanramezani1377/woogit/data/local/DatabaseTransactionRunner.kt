package com.samanramezani1377.woogit.data.local

import app.cash.sqldelight.db.SqlDriver

class DatabaseTransactionRunner(private val driver: SqlDriver) {
    fun <T> transaction(block: () -> T): T = driver.executeInTransactionWithResult(block)
}
