package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.ConnectionState
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.domain.model.StoreConnection
import com.samanramezani1377.woogit.core.domain.repository.StoreRepository
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.data.local.SqlStoreDataSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import java.net.URI

class StoreRepositoryImpl(private val local: SqlStoreDataSource, private val credentials: SecureCredentialStore, private val httpClient: HttpClient) : StoreRepository {
    override suspend fun get(id: StoreId): CoreResult<StoreConnection> = local.get(id)
    override suspend fun save(store: StoreConnection): CoreResult<StoreConnection> = if (store.baseUrl.isBlank()) CoreResult.Failure(DomainError.Validation("Store URL is required")) else { local.upsert(store); CoreResult.Success(store) }
    override suspend fun connect(store: StoreConnection, consumerKey: String, consumerSecret: String, wordpressUsername: String?, wordpressApplicationPassword: String?): CoreResult<StoreConnection> {
        if (consumerKey.isBlank() || consumerSecret.isBlank()) return CoreResult.Failure(DomainError.Authentication("WooCommerce credentials are required"))
        if (wordpressUsername.isNullOrBlank() || wordpressApplicationPassword.isNullOrBlank()) return CoreResult.Failure(DomainError.Authentication("نام کاربری وردپرس و Application Password برای مدیریت تصاویر الزامی است."))
        val normalized = runCatching { val uri=URI(store.baseUrl.trim().removeSuffix("/")); require(uri.scheme.equals("https",true)){"HTTPS is required"}; require(!uri.host.isNullOrBlank()){"A valid store host is required"}; uri.toString().removeSuffix("/") }.getOrElse { return CoreResult.Failure(DomainError.Validation(it.message ?: "Invalid store URL")) }
        return runCatching { httpClient.get("$normalized/wp-json/wc/v3/system_status") }.fold(
            onSuccess = { val reference=store.credentialReference?:CredentialReference("store-${store.storeId.value}"); credentials.put(reference,consumerKey,consumerSecret,wordpressUsername,wordpressApplicationPassword); val connected=store.copy(baseUrl=normalized,state=ConnectionState.CONNECTED,credentialReference=reference); local.upsert(connected); CoreResult.Success(connected) },
            onFailure = { CoreResult.Failure(DomainError.Network(it.message ?: "Unable to connect to store")) }
        )
    }
    override suspend fun disconnect(id: StoreId): CoreResult<Unit> { val current=local.get(id); if(current is CoreResult.Success){ current.value.credentialReference?.let(credentials::remove); local.upsert(current.value.copy(state=ConnectionState.DISCONNECTED,credentialReference=null)) }; return CoreResult.Success(Unit) }
}
