package dev.kodex.server.domain.auth.service

interface PasswordHasherPort {
    fun hash(plain: String): String
    fun verify(hash: String, plain: String): Boolean
}
