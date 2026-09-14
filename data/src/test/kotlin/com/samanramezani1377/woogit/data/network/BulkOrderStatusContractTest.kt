package com.samanramezani1377.woogit.data.network

import kotlin.test.Test
import kotlin.test.assertEquals

class BulkOrderStatusContractTest {
    @Test
    fun mapperPreservesRequestedOrderCount() {
        val response = ApiResponse(
            statusCode = 200,
            body = "{\"update\":[{\"id\":301,\"status\":\"processing\"}]}",
        )

        val result = BulkOrderStatusMapper.map(response, listOf(301, 302, 303))

        assertEquals(3, result.size)
    }
}
