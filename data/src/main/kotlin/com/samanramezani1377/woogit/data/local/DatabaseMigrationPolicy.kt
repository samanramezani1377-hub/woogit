package com.samanramezani1377.woogit.data.local

/**
 * Policy for database migration failures.
 *
 * V1 never silently continues with a partially migrated database and never
 * deletes business data as an automatic recovery action. The caller must
 * surface the failure and keep the existing database untouched where the
 * underlying driver supports transactional migration.
 */
sealed interface DatabaseMigrationResult {
    data class Success(val version: Int) : DatabaseMigrationResult
    data class Failure(val version: Int, val cause: Throwable) : DatabaseMigrationResult
}

fun interface DatabaseMigrationPolicy {
    fun onFailure(version: Int, cause: Throwable): DatabaseMigrationResult.Failure
}

object FailClosedMigrationPolicy : DatabaseMigrationPolicy {
    override fun onFailure(version: Int, cause: Throwable): DatabaseMigrationResult.Failure =
        DatabaseMigrationResult.Failure(version, cause)
}
