package dev.kodex.server.domain.passkey.repository

import kotlin.time.Instant

data class PasskeyRecord(
    val id: Long,
    val userId: Long,
    val credentialId: ByteArray,
    val publicKeyCose: ByteArray,
    val signCount: Long,
    val aaguid: String?,
    val friendlyName: String?,
    val createdAt: Instant,
)

interface PasskeyRepository {
    suspend fun create(
        userId: Long,
        credentialId: ByteArray,
        publicKeyCose: ByteArray,
        signCount: Long,
        aaguid: String?,
        friendlyName: String?,
    ): PasskeyRecord

    suspend fun findByCredentialId(credentialId: ByteArray): PasskeyRecord?
    suspend fun findByUserId(userId: Long): List<PasskeyRecord>
    suspend fun updateSignCount(id: Long, signCount: Long)
    suspend fun delete(id: Long, userId: Long)
    suspend fun countForUser(userId: Long): Int
}
