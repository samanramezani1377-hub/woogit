package com.samanramezani1377.woogit.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncRecoveryPolicyTest {
    @Test
    fun runningOperationBecomesPendingAfterRestart() {
        assertEquals(SyncRecoveryPolicy.PENDING, SyncRecoveryPolicy.recoverState(SyncRecoveryPolicy.RUNNING))
    }

    @Test
    fun terminalStatesArePreserved() {
        assertEquals(SyncRecoveryPolicy.CONFLICT, SyncRecoveryPolicy.recoverState(SyncRecoveryPolicy.CONFLICT))
        assertEquals(SyncRecoveryPolicy.SUCCEEDED, SyncRecoveryPolicy.recoverState(SyncRecoveryPolicy.SUCCEEDED))
    }
}
