package dev.kodex.server.data.crypto

import java.security.SecureRandom
import kotlin.io.encoding.Base64
import org.koin.core.annotation.Single

@Single
class TotpCrypto(private val secureRandom: SecureRandom) {
    fun encrypt(plaintext: String, keyBase64: String): String =
        Base64.encode(aesGcmEncrypt(plaintext.toByteArray(Charsets.UTF_8), keyBase64, secureRandom))

    fun decrypt(ciphertextBase64: String, keyBase64: String): String =
        String(aesGcmDecrypt(Base64.decode(ciphertextBase64), keyBase64), Charsets.UTF_8)
}
