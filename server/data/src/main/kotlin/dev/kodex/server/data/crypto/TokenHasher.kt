package dev.kodex.server.data.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import org.koin.core.annotation.Single

private const val TOKEN_BYTES = 32

@Single
class TokenHasher(private val secureRandom: SecureRandom) {
    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return bytes.toHexString()
    }

    fun hash(rawToken: String): String = MessageDigest.getInstance("SHA-256")
        .digest(rawToken.toByteArray(Charsets.UTF_8))
        .toHexString()

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
