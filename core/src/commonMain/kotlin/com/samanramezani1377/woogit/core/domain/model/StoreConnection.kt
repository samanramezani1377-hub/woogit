package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.StoreId

@JvmInline
value class CredentialReference(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class StoreConnection(
    val storeId: StoreId,
    val baseUrl: String,
    val state: ConnectionState,
    val credentialReference: CredentialReference?
)
