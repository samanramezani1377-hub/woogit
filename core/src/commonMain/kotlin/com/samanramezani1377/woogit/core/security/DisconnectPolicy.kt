package com.samanramezani1377.woogit.core.security

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.CredentialReference

interface DisconnectPolicy {
    suspend fun disconnect(storeId: StoreId, credentialReference: CredentialReference): DisconnectResult
}

enum class DisconnectResult {
    DISCONNECTED,
    CREDENTIAL_ALREADY_ABSENT
}

/** V1 keeps cached store data unless a separate data-retention policy explicitly removes it. */
interface StoreConnectionStateWriter {
    suspend fun markDisconnected(storeId: StoreId)
}
