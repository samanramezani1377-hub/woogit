package com.samanramezani1377.woogit.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecureCredentialStore(context: Context) : SecureCredentialStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun put(reference: CredentialReference, consumerKey: String, consumerSecret: String, wordpressUsername: String?, wordpressApplicationPassword: String?) {
        require(consumerKey.isNotBlank()) { "Consumer key cannot be blank" }
        require(consumerSecret.isNotBlank()) { "Consumer secret cannot be blank" }
        if (!wordpressUsername.isNullOrBlank()) require(!wordpressApplicationPassword.isNullOrBlank()) { "WordPress Application Password is required" }
        if (!wordpressApplicationPassword.isNullOrBlank()) require(!wordpressUsername.isNullOrBlank()) { "WordPress username is required" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(reference))
        val encrypted = cipher.doFinal(listOf(consumerKey, consumerSecret, wordpressUsername.orEmpty(), wordpressApplicationPassword.orEmpty()).joinToString(SEP).toByteArray(StandardCharsets.UTF_8))
        preferences.edit().putString(ivKey(reference), Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).putString(ciphertextKey(reference), Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    override fun get(reference: CredentialReference): CredentialPair? {
        val iv = preferences.getString(ivKey(reference), null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val ciphertext = preferences.getString(ciphertextKey(reference), null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val key = keyStore.getKey(alias(reference), null) as? SecretKey ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val parts = String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8).split(SEP)
            require(parts.size == 2 || parts.size == 4) { "Invalid credential payload" }
            CredentialPair(parts[0], parts[1], parts.getOrNull(2)?.ifBlank { null }, parts.getOrNull(3)?.ifBlank { null })
        }.getOrNull()
    }

    override fun remove(reference: CredentialReference) {
        preferences.edit().remove(ivKey(reference)).remove(ciphertextKey(reference)).apply()
        if (keyStore.containsAlias(alias(reference))) keyStore.deleteEntry(alias(reference))
    }

    private fun getOrCreateKey(reference: CredentialReference): SecretKey {
        (keyStore.getKey(alias(reference), null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply { init(KeyGenParameterSpec.Builder(alias(reference), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).setKeySize(256).build()) }.generateKey()
    }
    private fun alias(reference: CredentialReference): String = "woogit.credential.${sha256(reference.value)}"
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun ivKey(reference: CredentialReference) = "iv.${sha256(reference.value)}"
    private fun ciphertextKey(reference: CredentialReference) = "ciphertext.${sha256(reference.value)}"
    private companion object { const val ANDROID_KEYSTORE = "AndroidKeyStore"; const val TRANSFORMATION = "AES/GCM/NoPadding"; const val GCM_TAG_BITS = 128; const val PREFERENCES = "woogit_secure_credentials"; const val SEP = "\u0000" }
}
