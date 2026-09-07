package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.repository.ImageFetcher
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import java.net.URI
import java.util.Base64

class DirectCustomerImageFetcher(private val db: WooGitDatabase, private val credentials: SecureCredentialStore, private val httpClient: HttpClient) : ImageFetcher {
    override suspend fun fetch(storeId: StoreId, sourceUrl: String): ByteArray {
        val row = db.storeQueries.selectById(storeId.value).executeAsOneOrNull() ?: throw IllegalArgumentException("Store is not configured")
        val source = URI(sourceUrl.trim()); val store = URI(row.base_url.trimEnd('/'))
        require(source.scheme.equals("https", true)) { "Direct image download requires HTTPS" }; require(source.userInfo.isNullOrBlank()) { "Image URL must not contain credentials" }
        require(!source.host.isNullOrBlank()) { "Image URL host is required" }; require(source.host.equals(store.host, true)) { "Image URL is outside the bound Customer Site" }; require(source.port == -1 || source.port == 443) { "Image URL must use HTTPS/443" }
        val reference = row.credential_reference ?: throw IllegalStateException("Store credentials are unavailable")
        val pair = credentials.get(CredentialReference(reference)) ?: throw IllegalStateException("Store credentials are unavailable")
        val auth = if (!pair.wordpressUsername.isNullOrBlank() && !pair.wordpressApplicationPassword.isNullOrBlank()) { val token = Base64.getEncoder().encodeToString("${pair.wordpressUsername}:${pair.wordpressApplicationPassword}".toByteArray(Charsets.UTF_8)); "Basic $token" } else null
        val response = httpClient.get(Url(sourceUrl)) { auth?.let { header(HttpHeaders.Authorization, it) }; header(HttpHeaders.Accept, "image/*") }
        if (response.status.value !in 200..299) throw HttpApiException(response.status.value, response.bodyAsText())
        return response.readBytes()
    }
}
