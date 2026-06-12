package dev.kodex.server.domain.auth.repository

import dev.kodex.core.models.auth.OAuthProvider
import dev.kodex.server.domain.auth.model.OAuthIdentityRecord

interface OAuthIdentityRepository {
    suspend fun findByProvider(provider: OAuthProvider, providerUserId: String): OAuthIdentityRecord?
    suspend fun findByUserId(userId: Long): List<OAuthIdentityRecord>
    suspend fun create(userId: Long, provider: OAuthProvider, providerUserId: String): OAuthIdentityRecord
    suspend fun delete(userId: Long, provider: OAuthProvider)
    suspend fun countForUser(userId: Long): Int
}
