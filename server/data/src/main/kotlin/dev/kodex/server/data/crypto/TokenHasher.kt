package dev.kodex.server.data.crypto

import java.security.MessageDigest
import java.security.SecureRandom

object TokenHasher {
    private val secureRandom = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.toHexString()
    }

    fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawToken.toByteArray(Charsets.UTF_8)).toHexString()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
