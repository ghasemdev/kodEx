package dev.kodex.server.data.service

import dev.kodex.server.data.crypto.PasswordHasher
import dev.kodex.server.domain.auth.service.PasswordHasherPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [PasswordHasherPort::class])
class PasswordHasherPortImpl : PasswordHasherPort {
    override suspend fun hash(plain: String): String = withContext(Dispatchers.Default) {
        PasswordHasher.hash(plain)
    }

    override suspend fun verify(hash: String, plain: String): Boolean = withContext(Dispatchers.Default) {
        PasswordHasher.verify(hash, plain)
    }
}
