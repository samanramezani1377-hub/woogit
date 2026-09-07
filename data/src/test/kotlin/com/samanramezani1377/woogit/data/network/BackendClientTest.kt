package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.BackendSessionStore
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendClientTest {
    private val pair = CredentialPair("ck_test", "cs_test", "wp_user", "wp_app_password")

    @Test
    fun verifySiteUsesStableIdempotencyKeyAcrossRetries() = kotlinx.coroutines.test.runTest {
        val keys = mutableListOf<String?>()
        val engine = MockEngine { request: HttpRequestData ->
            keys += request.headers["Idempotency-Key"]
            respond(
                content = "{\"session\":\"session-1\",\"scope\":\"operational\",\"access_enabled\":\"true\"}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val sessions = FakeSessionStore()
        BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.0.0").let { client ->
            assertTrue(client.verifySite("store-1", "https://shop.example", pair).isSuccess)
            assertTrue(client.verifySite("store-1", "https://shop.example", pair).isSuccess)
        }
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
        assertTrue(keys[0]!!.startsWith("verify-"))
        assertTrue(keys[0]!!.length > 10)
    }

    @Test
    fun verifySiteChangesIdempotencyKeyWhenLogicalInputsChange() = kotlinx.coroutines.test.runTest {
        val keys = mutableListOf<String?>()
        val engine = MockEngine { request ->
            keys += request.headers["Idempotency-Key"]
            respond("{\"session\":\"s\",\"scope\":\"operational\"}")
        }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), FakeSessionStore(), "1.0.0")
        client.verifySite("store-1", "https://shop.example", pair)
        client.verifySite("store-1", "https://shop.example", pair.copy(consumerSecret = "different"))
        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun mutationForwardCarriesBackendSessionCredentialsVersionAndIdempotency() = kotlinx.coroutines.test.runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond("{\"id\":1}")
        }
        val sessions = FakeSessionStore().also { it.put("store-1", "session-token") }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.2.3")
        val response = client.forward("store-1", "/wc/v3/products", "post", pair, mapOf("page" to 2), "{\"name\":\"Test\"}")
        assertEquals(200, response.status)
        assertEquals("POST", captured!!.method.value)
        assertEquals("session-token", captured!!.headers["X-WooGit-Session"])
        assertEquals("1.2.3", captured!!.headers["X-WooGit-App-Version"])
        assertEquals("ck_test", captured!!.headers["X-WooGit-Consumer-Key"])
        assertEquals("cs_test", captured!!.headers["X-WooGit-Consumer-Secret"])
        assertEquals("wp_user", captured!!.headers["X-WooGit-Wordpress-Username"])
        assertTrue(captured!!.url.toString().contains("path=%2Fwc%2Fv3%2Fproducts") || captured!!.url.toString().contains("path=/wc/v3/products"))
        assertTrue(captured!!.headers["Idempotency-Key"]!!.startsWith("app-"))
    }

    @Test
    fun readOnlyForwardDoesNotSendIdempotencyKey() = kotlinx.coroutines.test.runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request -> captured = request; respond("[]") }
        val sessions = FakeSessionStore().also { it.put("store-1", "session-token") }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.0.0")
        client.forward("store-1", "/wc/v3/products", "GET", pair)
        assertNull(captured!!.headers["Idempotency-Key"])
    }

    @Test
    fun unauthorizedForwardClearsStoredSession() = kotlinx.coroutines.test.runTest {
        val engine = MockEngine { respond("unauthorized", HttpStatusCode.Unauthorized) }
        val sessions = FakeSessionStore().also { it.put("store-1", "session-token") }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.0.0")
        client.forward("store-1", "/wc/v3/products", "GET", pair)
        assertNull(sessions.get("store-1"))
    }

    @Test
    fun binaryForwardRequiresMutationMethod() = kotlinx.coroutines.test.runTest {
        val engine = MockEngine { respond("bad") }
        val sessions = FakeSessionStore().also { it.put("store-1", "session-token") }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.0.0")
        assertFailsWith<IllegalArgumentException> {
            client.forwardBinary("store-1", "/wp/v2/media", "GET", pair, bytes = byteArrayOf(1, 2), contentType = "image/png", fileName = "x.png")
        }
    }

    @Test
    fun binaryForwardIdempotencyChangesWhenPayloadChanges() = kotlinx.coroutines.test.runTest {
        val keys = mutableListOf<String?>()
        val engine = MockEngine { request ->
            keys += request.headers["Idempotency-Key"]
            respond("{}")
        }
        val sessions = FakeSessionStore().also { it.put("store-1", "session-token") }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.0.0")
        client.forwardBinary("store-1", "/wp/v2/media", "POST", pair, bytes = byteArrayOf(1), contentType = "image/png", fileName = "x.png")
        client.forwardBinary("store-1", "/wp/v2/media", "POST", pair, bytes = byteArrayOf(2), contentType = "image/png", fileName = "x.png")
        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun operationLookupUsesSessionAndParsesStatus() = kotlinx.coroutines.test.runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond("{\"operation_id\":\"op/1\",\"status\":\"unknown\",\"response\":{\"ok\":true}}")
        }
        val sessions = FakeSessionStore().also { it.put("store-1", "session-token") }
        val client = BackendClient(HttpClient(engine), "https://woogit.ir", FakeCredentialStore(), sessions, "1.0.0")
        val result = client.getOperation("store-1", "op/1")
        assertEquals("op/1", result.operationId)
        assertEquals("unknown", result.status)
        assertEquals("session-token", captured!!.headers["X-WooGit-Session"])
        assertTrue(captured!!.url.toString().contains("op%2F1"))
    }

    private class FakeSessionStore : BackendSessionStore {
        private val values = mutableMapOf<String, String>()
        override fun get(storeId: String): String? = values[storeId]
        override fun put(storeId: String, token: String) { values[storeId] = token }
        override fun remove(storeId: String) { values.remove(storeId) }
    }

    private class FakeCredentialStore : SecureCredentialStore {
        override fun put(reference: com.samanramezani1377.woogit.core.domain.model.CredentialReference, consumerKey: String, consumerSecret: String, wordpressUsername: String?, wordpressApplicationPassword: String?) = Unit
        override fun get(reference: com.samanramezani1377.woogit.core.domain.model.CredentialReference): CredentialPair? = null
        override fun remove(reference: com.samanramezani1377.woogit.core.domain.model.CredentialReference) = Unit
    }
}
