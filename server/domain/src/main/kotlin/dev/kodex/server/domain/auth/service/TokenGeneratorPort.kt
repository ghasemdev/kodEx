package dev.kodex.server.domain.auth.service

interface TokenGeneratorPort {
    fun generate(): String
    fun hash(raw: String): String
}
