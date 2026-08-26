package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.CredentialPair
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class NetworkClient(
    private val policy: RequestPolicy = RequestPolicy()
) {
    val httpClient: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = policy.timeout.inWholeMilliseconds
            connectTimeoutMillis = 10.seconds.inWholeMilliseconds
            socketTimeoutMillis = policy.timeout.inWholeMilliseconds
        }
    }

    suspend fun execute(
        method: HttpMethod,
        url: String,
        credentials: CredentialPair,
        configure: HttpRequestBuilder.() -> Unit = {}
    ): Result<HttpResponse> = runCatching {
        require(url.startsWith("https://", ignoreCase = true)) { "HTTPS is required" }
        httpClient.request(url) {
            this.method = method
            header(HttpHeaders.Authorization, WooCommerceRequestBuilder().basicAuthHeader(credentials))
            configure()
        }
    }

    fun close() = httpClient.close()
}
