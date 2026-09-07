package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.ConnectionState
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.domain.model.StoreConnection
import com.samanramezani1377.woogit.core.domain.repository.StoreRepository
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.data.local.SqlStoreDataSource
import com.samanramezani1377.woogit.data.network.BackendClient
import java.net.URI

class StoreRepositoryImpl(
    private val local: SqlStoreDataSource,
    private val credentials: SecureCredentialStore,
    private val backend: BackendClient,
) : StoreRepository {
    override suspend fun get(id: StoreId): CoreResult<StoreConnection> = local.get(id)
    override suspend fun save(store: StoreConnection): CoreResult<StoreConnection> = if (store.baseUrl.isBlank()) CoreResult.Failure(DomainError.Validation("Store URL is required")) else { local.upsert(store); CoreResult.Success(store) }

    override suspend fun connect(store: StoreConnection, consumerKey: String, consumerSecret: String, wordpressUsername: String?, wordpressApplicationPassword: String?): CoreResult<StoreConnection> {
        val parts=consumerSecret.split(SECRET_SEPARATOR)
        val actualSecret=parts[0]
        val actualWpUser=wordpressUsername?:parts.getOrNull(1)
        val actualWpPassword=wordpressApplicationPassword?:parts.getOrNull(2)
        if(consumerKey.isBlank()||actualSecret.isBlank()) return CoreResult.Failure(DomainError.Authentication("WooCommerce credentials are required"))
        if(actualWpUser.isNullOrBlank()||actualWpPassword.isNullOrBlank()) return CoreResult.Failure(DomainError.Authentication("نام کاربری وردپرس و Application Password برای مدیریت تصاویر الزامی است."))
        val normalized=runCatching{normalizeBaseUrl(store.baseUrl)}.getOrElse{return CoreResult.Failure(DomainError.Validation(it.message?:"Invalid store URL"))}
        val reference=store.credentialReference?:CredentialReference("store-${store.storeId.value}")
        val pair=CredentialPair(consumerKey,actualSecret,actualWpUser,actualWpPassword)
        return backend.verifySite(store.storeId.value,normalized,pair).fold(
            onSuccess={
                credentials.put(reference,consumerKey,actualSecret,actualWpUser,actualWpPassword)
                val connected=store.copy(baseUrl=normalized,state=ConnectionState.CONNECTED,credentialReference=reference)
                local.upsert(connected)
                CoreResult.Success(connected)
            },
            onFailure={CoreResult.Failure(DomainError.Network(it.message?:"Unable to verify store through WooGit Backend"))}
        )
    }

    override suspend fun disconnect(id: StoreId): CoreResult<Unit> {
        val current=local.get(id)
        if(current is CoreResult.Success){ current.value.credentialReference?.let(credentials::remove); backend.clearSession(id.value); local.upsert(current.value.copy(state=ConnectionState.DISCONNECTED,credentialReference=null)) }
        return CoreResult.Success(Unit)
    }

    private fun normalizeBaseUrl(value:String):String{
        val uri=URI(if(value.trim().startsWith("http",true)) value.trim() else "https://${value.trim()}")
        require(uri.scheme.equals("https",true)){"Customer Site must use HTTPS"}
        require(uri.host.isNullOrBlank().not()){ "A valid store host is required" }
        require(uri.port==-1||uri.port==443){"Customer Site must use HTTPS on port 443"}
        require(uri.userInfo.isNullOrBlank()){"Customer Site URL must not contain credentials"}
        require(uri.path.isNullOrBlank()||uri.path=="/"){"Customer Site URL must be origin-only"}
        return "https://${uri.host!!.lowercase()}"
    }
    private companion object { const val SECRET_SEPARATOR="\u0001" }
}
