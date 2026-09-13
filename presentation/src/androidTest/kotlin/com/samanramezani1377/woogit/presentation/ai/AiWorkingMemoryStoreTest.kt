package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
            JSONObject()
                .put("completed", 1)
                .put("remaining", 2),
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
                .put(
                    "progress",
                    JSONObject()
                        .put("completed", 3)
                        .put("remaining", 4),
                ),
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
    fun inFlightMutationSurvivesResumeAndIsNotMarkedVerifiedPrematurely() {
        val initial = store.ensure(CONVERSATION_ID, "تغییر محصول")
        val executionId = initial.getString("executionId")

        store.beginInFlightOperation(
            CONVERSATION_ID,
            JSONObject()
                .put("tool", "products_update")
                .put(
                    "arguments",
                    "{\"id\":10,\"status\":\"publish\"}",
                ),
        )

        val stopped = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing")
        assertEquals(executionId, stopped.getString("executionId"))
        assertEquals(
            "in_flight",
            stopped.getJSONObject("checkpoint").getString("status"),
        )

        store.resume(CONVERSATION_ID)

        val resumed = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing after resume")
        assertEquals(executionId, resumed.getString("executionId"))
        assertEquals(
            "in_flight",
            resumed.getJSONObject("checkpoint").getString("status"),
        )

        store.recordOperation(
            CONVERSATION_ID,
            JSONObject()
                .put("tool", "products_update")
                .put(
                    "arguments",
                    "{\"id\":10,\"status\":\"publish\"}",
                )
                .put("status", "verified"),
        )

        val verified = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing after verification")
        assertEquals(
            "verified",
            verified.getJSONObject("checkpoint").getString("status"),
        )
        assertEquals(1, verified.getJSONArray("operations").length())
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

        val state = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing")
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

        val snapshot = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing")
        assertEquals(8, snapshot.getJSONArray("operations").length())
        assertEquals(
            "{\"page\":4}",
            snapshot.getJSONArray("operations")
                .getJSONObject(0)
                .getString("arguments"),
        )
        assertNotEquals(0, snapshot.getString("executionId").length)
    }

    @Test
    fun batchSelectionPersistsRejectedAndVerifiedItems() {
        store.ensure(CONVERSATION_ID, "افزایش قیمت چند محصول")
        val items = JSONArray()
            .put(
                JSONObject()
                    .put("itemId", "a")
                    .put("tool", "products_update")
                    .put("status", "PENDING_CONFIRMATION"),
            )
            .put(
                JSONObject()
                    .put("itemId", "b")
                    .put("tool", "products_update")
                    .put("status", "PENDING_CONFIRMATION"),
            )

        store.createBatch(
            CONVERSATION_ID,
            "batch-1",
            "افزایش قیمت چند محصول",
            items,
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-1",
            "a",
            "REJECTED_BY_USER",
            "کاربر رد کرد",
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-1",
            "b",
            "VERIFIED",
            "تأیید شد",
        )

        val state = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing")
        val batch = state.getJSONArray("batches").getJSONObject(0)
        val persisted = batch.getJSONArray("items")

        assertEquals(
            "REJECTED_BY_USER",
            persisted.getJSONObject(0).getString("status"),
        )
        assertEquals("VERIFIED", persisted.getJSONObject(1).getString("status"))
        assertEquals("completed", batch.getString("status"))
    }

    @Test
    fun batchKeepsExactMixedOutcomeForVerifiedFailedAndRejectedItems() {
        store.ensure(CONVERSATION_ID, "اجرای ترکیبی محصولات")
        val items = JSONArray()
            .put(
                JSONObject()
                    .put("itemId", "verified")
                    .put("tool", "products_update")
                    .put("label", "محصول موفق")
                    .put("status", "PENDING_CONFIRMATION"),
            )
            .put(
                JSONObject()
                    .put("itemId", "failed")
                    .put("tool", "products_update")
                    .put("label", "محصول ناموفق")
                    .put("status", "PENDING_CONFIRMATION"),
            )
            .put(
                JSONObject()
                    .put("itemId", "rejected")
                    .put("tool", "products_update")
                    .put("label", "محصول ردشده")
                    .put("status", "PENDING_CONFIRMATION"),
            )

        store.createBatch(
            CONVERSATION_ID,
            "batch-mixed",
            "اجرای ترکیبی محصولات",
            items,
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-mixed",
            "verified",
            "VERIFIED",
            "ok=true verified=true",
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-mixed",
            "failed",
            "FAILED",
            "HTTP 400: invalid stock_status",
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-mixed",
            "rejected",
            "REJECTED_BY_USER",
            "کاربر انتخاب نکرد",
        )

        val state = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing")
        val batch = state.getJSONArray("batches")
            .getJSONObject(0)
        val persisted = batch.getJSONArray("items")

        assertEquals("VERIFIED", persisted.getJSONObject(0).getString("status"))
        assertEquals(
            "ok=true verified=true",
            persisted.getJSONObject(0).getString("result"),
        )
        assertEquals("FAILED", persisted.getJSONObject(1).getString("status"))
        assertEquals(
            "HTTP 400: invalid stock_status",
            persisted.getJSONObject(1).getString("result"),
        )
        assertEquals(
            "REJECTED_BY_USER",
            persisted.getJSONObject(2).getString("status"),
        )
        assertEquals("completed_with_failures", batch.getString("status"))
    }

    @Test
    fun batchRecoveryDoesNotLoseRejectedOrFailedOutcomeAfterResume() {
        store.ensure(CONVERSATION_ID, "بازیابی batch")
        val items = JSONArray()
            .put(
                JSONObject()
                    .put("itemId", "done")
                    .put("tool", "products_update")
                    .put("status", "PENDING_CONFIRMATION"),
            )
            .put(
                JSONObject()
                    .put("itemId", "failed")
                    .put("tool", "products_update")
                    .put("status", "PENDING_CONFIRMATION"),
            )
            .put(
                JSONObject()
                    .put("itemId", "rejected")
                    .put("tool", "products_update")
                    .put("status", "PENDING_CONFIRMATION"),
            )

        store.createBatch(
            CONVERSATION_ID,
            "batch-recovery",
            "بازیابی batch",
            items,
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-recovery",
            "done",
            "VERIFIED",
            "verified",
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-recovery",
            "failed",
            "FAILED",
            "خطای فروشگاه",
        )
        store.updateBatchItem(
            CONVERSATION_ID,
            "batch-recovery",
            "rejected",
            "REJECTED_BY_USER",
            "کاربر انتخاب نکرد",
        )

        store.resume(CONVERSATION_ID)

        val state = store.read(CONVERSATION_ID)
            ?: error("Working Memory missing after batch resume")
        val batch = state.getJSONArray("batches")
            .getJSONObject(0)
        val persisted = batch.getJSONArray("items")

        assertEquals("VERIFIED", persisted.getJSONObject(0).getString("status"))
        assertEquals("FAILED", persisted.getJSONObject(1).getString("status"))
        assertEquals(
            "REJECTED_BY_USER",
            persisted.getJSONObject(2).getString("status"),
        )
        assertEquals("completed_with_failures", batch.getString("status"))
    }

    private companion object {
        const val CONVERSATION_ID = "working-memory-regression"
    }
}
