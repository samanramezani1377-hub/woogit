package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiWorkingMemoryStoreTest {
    private lateinit var context: Context
    private lateinit var store: AiWorkingMemoryStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = AiWorkingMemoryStore(context, "working-memory-test")
        store.clear(CONVERSATION_ID)
    }

    @After
    fun tearDown() {
        store.clear(CONVERSATION_ID)
    }

    @Test
    fun ensureKeepsExistingExecutionAfterCompletion() {
        val initial = store.ensure(CONVERSATION_ID, "یک کار چندمرحله‌ای")
        val executionId = initial.getString("executionId")

        store.recordOperation(
            CONVERSATION_ID,
            JSONObject()
                .put("name", "products_list")
                .put("arguments", "{\"page\":1}")
                .put("status", "completed"),
            JSONObject().put("completed", 1).put("remaining", 2),
        )
        store.complete(CONVERSATION_ID, "مرحله اول انجام شد")

        val restored = store.ensure(CONVERSATION_ID, "درخواست بعدی")
        assertEquals(executionId, restored.getString("executionId"))
        assertEquals("completed", restored.getString("status"))
        assertEquals(1, restored.getJSONArray("operations").length())
    }

    @Test
    fun resumeReopensSameExecutionWithoutCreatingNewIdentity() {
        val initial = store.ensure(CONVERSATION_ID, "کار قابل ادامه")
        val executionId = initial.getString("executionId")
        store.checkpoint(
            CONVERSATION_ID,
            JSONObject()
                .put("checkpoint", JSONObject().put("page", 3))
                .put("progress", JSONObject().put("completed", 3).put("remaining", 4)),
        )
        store.complete(CONVERSATION_ID, "تا صفحه ۳ انجام شد")

        val resumed = store.resume(CONVERSATION_ID)
        assertEquals(executionId, resumed.getString("executionId"))
        assertEquals("active", resumed.getString("status"))
        assertEquals(3, resumed.getJSONObject("progress").getInt("completed"))
        assertEquals(4, resumed.getJSONObject("progress").getInt("remaining"))
        assertEquals(3, resumed.getJSONObject("checkpoint").getInt("page"))
    }

    @Test
    fun repeatedOperationIsIdempotent() {
        val initial = store.ensure(CONVERSATION_ID, "کار تکرارشونده")
        val executionId = initial.getString("executionId")
        val operation = JSONObject()
            .put("name", "products_update")
            .put("arguments", "{\"id\":10,\"patch\":{\"status\":\"publish\"}}")
            .put("status", "verified")

        store.recordOperation(CONVERSATION_ID, operation)
        store.recordOperation(CONVERSATION_ID, operation)

        val state = store.read(CONVERSATION_ID) ?: error("Working Memory missing")
        assertEquals(executionId, state.getString("executionId"))
        assertEquals(1, state.getJSONArray("operations").length())
    }

    @Test
    fun promptReadIsCompactWhilePersistedHistoryRemainsAvailableForMutation() {
        store.ensure(CONVERSATION_ID, "کار طولانی")
        repeat(12) { index ->
            store.recordOperation(
                CONVERSATION_ID,
                JSONObject()
                    .put("name", "products_list")
                    .put("arguments", "{\"page\":$index}")
                    .put("status", "completed"),
            )
        }

        val snapshot = store.read(CONVERSATION_ID) ?: error("Working Memory missing")
        assertEquals(8, snapshot.getJSONArray("operations").length())
        assertTrue(snapshot.getJSONArray("operations").getJSONObject(0).getString("arguments").contains("page\\\":4"))
        assertNotEquals(0, snapshot.getString("executionId").length)
    }

    private companion object {
        const val CONVERSATION_ID = "working-memory-regression"
    }
}
