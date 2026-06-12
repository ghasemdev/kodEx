package dev.kodex.server.di

import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class JsonModule {
    @Single
    fun appJson(): Json = Json { ignoreUnknownKeys = true }
}
