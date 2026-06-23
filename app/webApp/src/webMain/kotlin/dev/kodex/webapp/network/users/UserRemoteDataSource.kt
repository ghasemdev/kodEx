package dev.kodex.webapp.network.users

import dev.kodex.core.models.auth.UserDto

interface UserRemoteDataSource {
    suspend fun getMe(): UserDto
}
