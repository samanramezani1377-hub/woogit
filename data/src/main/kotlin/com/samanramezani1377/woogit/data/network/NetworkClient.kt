package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.debug.NoOpTechnicalErrorReporter
import com.samanramezani1377.woogit.core.debug.TechnicalErrorContext
import com.samanramezani1377.woogit.core.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.core.security.CredentialPair
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class NetworkClient(
    private val policy: RequestPolicy = RequestPolicy(),
    private val technicalErrorReporter: TechnicalErrorReporter = NoOpTechnicalErrorReporter,
) {
    val httpClient: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }) }
        install(HttpTimeout) {
            requestTimeoutMillis = policy.timeout.inWholeMilliseconds
            connectTimeoutMillis = 10.seconds.inWholeMilliseconds
            socketTimeoutMillis = policy.timeout.inWholeMilliseconds
        }
        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response -> response.status.value == 429 || response.status.value in 500..599 }
            retryOnExceptionIf { _, cause -> cause is java.io.IOException }
            exponentialDelay()
        }
    }

    suspend fun execute(
        method: HttpMethod,
        url: String,
        credentials: CredentialPair,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): Result<HttpResponse> = runCatching {
        require(url.startsWith("https://", true)) { "HTTPS is required" }
        httpClient.request(url) {
            this.method = method
            header(HttpHeaders.Authorization, WooCommerceRequestBuilder().basicAuthHeader(credentials))
            configure()
        }
    }.onFailure { throwable ->
        technicalErrorReporter.report(
            TechnicalErrorContext(
                feature = "Network",
                location = "NetworkClient.execute",
                operation = "HTTP ${method.value}",
                type = "NetworkException",
                httpMethod = method.value,
                endpoint = sanitizeUrl(url),
                details = "Request execution failed",
            ),
            throwable,
        )
    }

    fun close() = httpClient.close()

    private fun sanitizeUrl(url: String): String = url.substringBefore('?')
}
