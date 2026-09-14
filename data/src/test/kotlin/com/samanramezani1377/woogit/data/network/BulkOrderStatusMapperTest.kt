package com.samanramezani1377.woogit.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BulkOrderStatusMapperTest {
    @Test
    fun mapsSuccessfulAndMissingBatchItemsIndividually() {
        val response = ApiResponse(
            statusCode = 200,
            body = "{\"update\":[{\"id\":101,\"status\":\"completed\"}]}",
        )

        val results = BulkOrderStatusMapper.map(response, listOf(101, 102))

        assertEquals(2, results.size)
        assertTrue(results.first { it.orderId.value == "101" }.succeeded)
        assertFalse(results.first { it.orderId.value == "102" }.succeeded)
    }

    @Test
    fun marksWholeBatchFailedForHttpError() {
        val response = ApiResponse(
            statusCode = 400,
            body = "{\"code\":\"rest_invalid_param\",\"message\":\"Invalid status\"}",
        )

        val results = BulkOrderStatusMapper.map(response, listOf(201, 202))

        assertEquals(2, results.size)
        assertTrue(results.all { !it.succeeded })
        assertTrue(results.all { it.error.orEmpty().contains("Invalid status") })
    }
}
