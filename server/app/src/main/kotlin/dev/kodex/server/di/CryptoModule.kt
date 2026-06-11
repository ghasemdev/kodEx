package dev.kodex.server.di

import java.security.SecureRandom
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class CryptoModule {
    @Single
    fun secureRandom(): SecureRandom = SecureRandom()
}
