package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.ConnectionState
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.domain.model.StoreConnection
import com.samanramezani1377.woogit.core.security.BackendSessionStore
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import io.ktor.client.HttpClient

class WooCommerceClientProvider(
    private val db: WooGitDatabase,
    private val credentials: SecureCredentialStore,
    private val httpClient: HttpClient,
    private val backend: BackendClient,
    private val sessions: BackendSessionStore,
) {
    fun client(storeId: StoreId): CoreResult<Pair<StoreConnection, TypedWooCommerceApi>> {
        val row=db.storeQueries.selectById(storeId.value).executeAsOneOrNull() ?: return CoreResult.Failure(DomainError.NotFound("store",storeId.value))
        val connection=StoreConnection(storeId,row.base_url,runCatching{ConnectionState.valueOf(row.connection_state)}.getOrDefault(ConnectionState.DISCONNECTED),row.credential_reference?.let(::CredentialReference))
        val ref=connection.credentialReference ?: return CoreResult.Failure(DomainError.Authentication("Store credentials are not configured"))
        val pair=credentials.get(ref) ?: return CoreResult.Failure(DomainError.Authentication("Store credentials are unavailable"))
        if(sessions.get(storeId.value).isNullOrBlank()) return CoreResult.Failure(DomainError.Authentication("WooGit Backend session is unavailable; reconnect the store"))
        return CoreResult.Success(connection to TypedWooCommerceApi(WooCommerceApi(backend,storeId.value,pair)))
    }
}
