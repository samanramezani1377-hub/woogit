package com.samanramezani1377.woogit.security

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.security.DisconnectPolicy
import com.samanramezani1377.woogit.core.security.DisconnectResult
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.core.security.StoreConnectionStateWriter

class AndroidDisconnectPolicy(
    private val credentialStore: SecureCredentialStore,
    private val connectionStateWriter: StoreConnectionStateWriter
) : DisconnectPolicy {
    override suspend fun disconnect(
        storeId: StoreId,
        credentialReference: CredentialReference
    ): DisconnectResult {
        credentialStore.remove(credentialReference)
        connectionStateWriter.markDisconnected(storeId)
        return DisconnectResult.DISCONNECTED
    }
}
