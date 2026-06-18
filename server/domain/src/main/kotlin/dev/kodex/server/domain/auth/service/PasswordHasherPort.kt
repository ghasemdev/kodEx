package dev.kodex.server.domain.auth.service

interface PasswordHasherPort {
    suspend fun hash(plain: String): String
    suspend fun verify(hash: String, plain: String): Boolean
}
