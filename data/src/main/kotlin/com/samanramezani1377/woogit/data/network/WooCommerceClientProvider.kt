package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.StoreConnection
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import io.ktor.client.HttpClient

class WooCommerceClientProvider(
    private val db: WooGitDatabase,
    private val credentials: SecureCredentialStore,
    private val httpClient: HttpClient,
) {
    fun client(storeId: StoreId): CoreResult<Pair<StoreConnection, TypedWooCommerceApi>> {
        val row = db.storeQueries.selectById(storeId.value).executeAsOneOrNull()
            ?: return CoreResult.Failure(DomainError.NotFound("store", storeId.value))

        val connection = StoreConnection(
            storeId = storeId,
            baseUrl = row.base_url,
            state = runCatching { com.samanramezani1377.woogit.core.domain.model.ConnectionState.valueOf(row.connection_state) }
                .getOrDefault(com.samanramezani1377.woogit.core.domain.model.ConnectionState.DISCONNECTED),
            credentialReference = row.credential_reference?.let(::com.samanramezani1377.woogit.core.domain.model.CredentialReference),
        )

        val reference = connection.credentialReference
            ?: return CoreResult.Failure(DomainError.Authentication("Store credentials are not configured"))
        val pair = credentials.get(reference)
            ?: return CoreResult.Failure(DomainError.Authentication("Store credentials are unavailable"))

        val api = TypedWooCommerceApi(WooCommerceApi(httpClient, pair))
        return CoreResult.Success(connection to api)
    }
}
