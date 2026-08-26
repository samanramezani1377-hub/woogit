package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.CredentialPair

class WooCommerceRequestBuilder {
    fun validateBaseUrl(baseUrl: String): Result<String> = runCatching {
        val normalized = baseUrl.trim().trimEnd('/')
        require(normalized.startsWith("https://", ignoreCase = true)) {
            "WooCommerce connections require HTTPS"
        }
        normalized
    }

    fun basicAuthHeader(credentials: CredentialPair): String {
        val encoded = java.util.Base64.getEncoder()
            .encodeToString("${credentials.consumerKey}:${credentials.consumerSecret}".toByteArray())
        return "Basic $encoded"
    }
}
