package dev.kodex.server.domain.passkey.repository

import dev.kodex.server.domain.passkey.model.PasskeyRecord

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
