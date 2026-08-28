package com.samanramezani1377.woogit.core.security

import com.samanramezani1377.woogit.core.domain.model.CredentialReference

/** Credentials are transient secrets and must never be part of domain persistence. */
interface SecureCredentialStore {
    fun put(reference: CredentialReference, consumerKey: String, consumerSecret: String, wordpressUsername: String? = null, wordpressApplicationPassword: String? = null)
    fun get(reference: CredentialReference): CredentialPair?
    fun remove(reference: CredentialReference)
}

data class CredentialPair(
    val consumerKey: String,
    val consumerSecret: String,
    val wordpressUsername: String? = null,
    val wordpressApplicationPassword: String? = null,
)
