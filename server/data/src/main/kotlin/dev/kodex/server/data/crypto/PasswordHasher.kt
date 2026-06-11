package dev.kodex.server.data.crypto

import de.mkammerer.argon2.Argon2Factory

private const val ITERATIONS = 2
private const val MEMORY_KIB = 65_536
private const val PARALLELISM = 4

object PasswordHasher {
    @Suppress("PropertyName", "RedundantSuppression")
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    fun hash(plain: String): String = argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, plain.toCharArray())

    fun verify(hash: String, plain: String): Boolean = argon2.verify(hash, plain.toCharArray())
}
