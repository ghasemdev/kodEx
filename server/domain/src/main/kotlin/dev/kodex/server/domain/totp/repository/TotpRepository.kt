package dev.kodex.server.domain.totp.repository

import dev.kodex.server.domain.totp.model.TotpRecord

interface TotpRepository {
    suspend fun findByUserId(userId: Long): TotpRecord?
    suspend fun create(userId: Long, secretEncrypted: String): TotpRecord
    suspend fun enable(userId: Long, backupCodesHashes: List<String>)
    suspend fun updateBackupCodes(userId: Long, remainingHashes: List<String>)
    suspend fun delete(userId: Long)
}
