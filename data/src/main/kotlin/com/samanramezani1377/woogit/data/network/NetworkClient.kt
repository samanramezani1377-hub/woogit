package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.debug.NoOpTechnicalErrorReporter
import com.samanramezani1377.woogit.core.debug.TechnicalErrorReporter
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Shared HTTP transport only.
 *
 * Customer WooCommerce/WordPress API requests must never use this class directly.
 * Normal Customer operations are owned by BackendClient and go through the WooGit
 * Backend /forward endpoint. The only V1 direct Customer exception (binary image
 * download) is isolated behind ImageFetcher/DirectCustomerImageFetcher.
 */
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

    fun close() = httpClient.close()
}
