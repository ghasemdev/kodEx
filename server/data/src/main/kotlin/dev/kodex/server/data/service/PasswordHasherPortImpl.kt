package dev.kodex.server.data.service

import dev.kodex.server.data.crypto.PasswordHasher
import dev.kodex.server.domain.auth.service.PasswordHasherPort
import org.koin.core.annotation.Single

@Single(binds = [PasswordHasherPort::class])
class PasswordHasherPortImpl : PasswordHasherPort {
    override fun hash(plain: String): String = PasswordHasher.hash(plain)
    override fun verify(hash: String, plain: String): Boolean = PasswordHasher.verify(hash, plain)
}
