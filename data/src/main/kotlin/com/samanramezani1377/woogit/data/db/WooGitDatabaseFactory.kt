package com.samanramezani1377.woogit.data.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object WooGitDatabaseFactory {
    fun create(context: Context): WooGitDatabase {
        val driver = AndroidSqliteDriver(
            schema = WooGitDatabase.Schema,
            context = context.applicationContext,
            name = "woogit.db"
        )
        return WooGitDatabase(driver)
    }
}
