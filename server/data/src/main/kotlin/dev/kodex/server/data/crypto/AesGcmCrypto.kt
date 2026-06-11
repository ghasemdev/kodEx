package dev.kodex.server.data.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

internal const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
internal const val GCM_IV_BYTES = 12
internal const val GCM_TAG_BITS = 128

internal fun aesGcmEncrypt(plaintext: ByteArray, keyBase64: String, secureRandom: SecureRandom): ByteArray {
    val key = SecretKeySpec(Base64.decode(keyBase64), "AES")
    val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return iv + cipher.doFinal(plaintext)
}

internal fun aesGcmDecrypt(combined: ByteArray, keyBase64: String): ByteArray {
    val key = SecretKeySpec(Base64.decode(keyBase64), "AES")
    val iv = combined.copyOfRange(0, GCM_IV_BYTES)
    val ciphertext = combined.copyOfRange(GCM_IV_BYTES, combined.size)
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(ciphertext)
}
