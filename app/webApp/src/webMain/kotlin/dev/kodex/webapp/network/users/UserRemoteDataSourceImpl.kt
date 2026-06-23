package dev.kodex.webapp.network.users

import dev.kodex.core.models.auth.UserDto
import dev.kodex.shared.coroutines.ioDispatcher
import dev.kodex.webapp.network.ApiRoutes
import dev.kodex.webapp.network.envelopeDataOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [UserRemoteDataSource::class])
class UserRemoteDataSourceImpl(private val client: HttpClient) : UserRemoteDataSource {
    override suspend fun getMe(): UserDto = withContext(ioDispatcher) {
        client.get(ApiRoutes.Users.ME).envelopeDataOrThrow()
    }
}
