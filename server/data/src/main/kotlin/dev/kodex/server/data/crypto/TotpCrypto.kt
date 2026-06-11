package dev.kodex.server.data.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

private const val AES_GCM = "AES/GCM/NoPadding"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_BITS = 128

object TotpCrypto {
    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: String, keyBase64: String): String {
        val keyBytes = Base64.decode(keyBase64)
        val key = SecretKeySpec(keyBytes, "AES")
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encode(iv + ciphertext)
    }

    fun decrypt(ciphertextBase64: String, keyBase64: String): String {
        val keyBytes = Base64.decode(keyBase64)
        val key = SecretKeySpec(keyBytes, "AES")
        val combined = Base64.decode(ciphertextBase64)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
