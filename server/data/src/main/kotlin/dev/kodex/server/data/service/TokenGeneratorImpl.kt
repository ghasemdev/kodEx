package dev.kodex.server.data.service

import dev.kodex.server.data.crypto.TokenHasher
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import org.koin.core.annotation.Single

@Single(binds = [TokenGeneratorPort::class])
class TokenGeneratorImpl(private val tokenHasher: TokenHasher) : TokenGeneratorPort {
    override fun generate(): String = tokenHasher.generate()
    override fun hash(raw: String): String = tokenHasher.hash(raw)
}
