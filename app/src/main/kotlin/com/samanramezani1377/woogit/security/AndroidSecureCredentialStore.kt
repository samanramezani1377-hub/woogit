package com.samanramezani1377.woogit.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.samanramezani1377.woogit.core.domain.model.CredentialReference
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecureCredentialStore : SecureCredentialStore {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun put(reference: CredentialReference, consumerKey: String, consumerSecret: String) {
        val key = getOrCreateKey(reference)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal("$consumerKey\u0000$consumerSecret".toByteArray(StandardCharsets.UTF_8))
        android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP)
        CredentialStorage.memory[reference.value] = StoredCredential(
            iv = cipher.iv.copyOf(),
            ciphertext = encrypted.copyOf()
        )
    }

    override fun get(reference: CredentialReference): CredentialPair? {
        val stored = CredentialStorage.memory[reference.value] ?: return null
        val key = keyStore.getKey(alias(reference), null) as? SecretKey ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, stored.iv))
        val value = String(cipher.doFinal(stored.ciphertext), StandardCharsets.UTF_8)
        val separator = value.indexOf('\u0000')
        if (separator <= 0) return null
        return CredentialPair(value.substring(0, separator), value.substring(separator + 1))
    }

    override fun remove(reference: CredentialReference) {
        CredentialStorage.memory.remove(reference.value)
        if (keyStore.containsAlias(alias(reference))) keyStore.deleteEntry(alias(reference))
    }

    private fun getOrCreateKey(reference: CredentialReference): SecretKey {
        (keyStore.getKey(alias(reference), null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias(reference),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun alias(reference: CredentialReference) = "woogit.credential.${reference.value}"

    private data class StoredCredential(val iv: ByteArray, val ciphertext: ByteArray)

    private object CredentialStorage {
        val memory = mutableMapOf<String, StoredCredential>()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
