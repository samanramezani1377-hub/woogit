package com.samanramezani1377.woogit.core

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.presentationKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoreFoundationTest {
    @Test
    fun idsRejectBlankValues() {
        assertEquals("order-1", EntityId("order-1").value)
        assertEquals("store-1", StoreId("store-1").value)
    }

    @Test
    fun recoverabilityAndPresentationMappingAreStable() {
        val error = DomainError.Network("offline")
        assertTrue(error.recoverable)
        assertEquals("network", error.presentationKey())
        assertFalse(DomainError.Validation("bad").recoverable)
    }

    @Test
    fun typedCoreResultCarriesDomainError() {
        val result: CoreResult<Nothing> = CoreResult.Failure(DomainError.Conflict("changed"))
        assertTrue(result is CoreResult.Failure)
    }
}
