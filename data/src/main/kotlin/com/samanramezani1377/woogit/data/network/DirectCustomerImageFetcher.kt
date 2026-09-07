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
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import java.net.InetAddress
import java.net.URI
import java.util.Base64

class DirectCustomerImageFetcher(private val db: WooGitDatabase, private val credentials: SecureCredentialStore, private val httpClient: HttpClient) : ImageFetcher {
    override suspend fun fetch(storeId: StoreId, sourceUrl: String): ByteArray {
        val row = db.storeQueries.selectById(storeId.value).executeAsOneOrNull() ?: throw IllegalArgumentException("Store is not configured")
        val source = URI(sourceUrl.trim()); val store = URI(row.base_url.trimEnd('/'))
        require(source.scheme.equals("https", true)) { "Direct image download requires HTTPS" }; require(source.userInfo.isNullOrBlank()) { "Image URL must not contain credentials" }
        val sourceHost = source.host
        require(!sourceHost.isNullOrBlank()) { "Image URL host is required" }; require(sourceHost.equals(store.host, true)) { "Image URL is outside the bound Customer Site" }; require(source.port == -1 || source.port == 443) { "Image URL must use HTTPS/443" }

        // The hostname is already bound to the verified Customer Site, but DNS can
        // still resolve it to a private/local address. Never make the direct image
        // exception a path into the device's local network.
        val addresses = InetAddress.getAllByName(sourceHost)
        require(addresses.isNotEmpty() && addresses.all(::isPublicAddress)) { "Image URL host resolves to a private or reserved address" }

        val reference = row.credential_reference ?: throw IllegalStateException("Store credentials are unavailable")
        val pair = credentials.get(CredentialReference(reference)) ?: throw IllegalStateException("Store credentials are unavailable")
        val auth = if (!pair.wordpressUsername.isNullOrBlank() && !pair.wordpressApplicationPassword.isNullOrBlank()) { val token = Base64.getEncoder().encodeToString("${pair.wordpressUsername}:${pair.wordpressApplicationPassword}".toByteArray(Charsets.UTF_8)); "Basic $token" } else null
        val response = httpClient.get(Url(sourceUrl)) { auth?.let { header(HttpHeaders.Authorization, it) }; header(HttpHeaders.Accept, "image/*") }
        if (response.status.value !in 200..299) throw HttpApiException(response.status.value, response.bodyAsText())
        val contentType = response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase()
        require(contentType?.startsWith("image/") == true) { "Customer image response is not an image" }
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        require(contentLength == null || contentLength <= MAX_IMAGE_BYTES) { "Customer image is too large" }
        val bytes = response.readRawBytes(MAX_IMAGE_BYTES + 1L)
        require(bytes.size <= MAX_IMAGE_BYTES) { "Customer image is too large" }
        return bytes
    }

    private fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address
        if (bytes.size == 16) {
            // IPv6 unique-local fc00::/7 and IPv4-mapped private/loopback addresses.
            if ((bytes[0].toInt() and 0xfe) == 0xfc) return false
            if (bytes.take(10).all { it == 0.toByte() } && bytes[10] == 0.toByte() && (bytes[11] == 0.toByte() || bytes[11] == 0xff.toByte())) {
                val v4 = bytes.copyOfRange(12, 16)
                if (v4[0].toInt() and 0xff == 127 || v4[0].toInt() and 0xff == 10 || (v4[0].toInt() and 0xff == 192 && v4[1].toInt() and 0xff == 168) || (v4[0].toInt() and 0xff == 172 && v4[1].toInt() and 0xff in 16..31)) return false
            }
        }
        if (bytes.size == 4) {
            val a = bytes[0].toInt() and 0xff; val b = bytes[1].toInt() and 0xff
            if (a == 10 || a == 127 || (a == 169 && b == 254) || (a == 172 && b in 16..31) || (a == 192 && b == 168)) return false
        }
        return true
    }

    private companion object { const val MAX_IMAGE_BYTES = 10L * 1024L * 1024L }
}
