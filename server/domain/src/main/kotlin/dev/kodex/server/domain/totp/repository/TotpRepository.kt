package dev.kodex.server.domain.totp.repository

import kotlin.time.Instant

data class TotpRecord(
    val id: Long,
    val userId: Long,
    val secretEncrypted: String,
    val enabled: Boolean,
    val backupCodesHashes: List<String>,
    val updatedAt: Instant,
)

interface TotpRepository {
    suspend fun findByUserId(userId: Long): TotpRecord?
    suspend fun create(userId: Long, secretEncrypted: String): TotpRecord
    suspend fun enable(userId: Long, backupCodesHashes: List<String>)
    suspend fun updateBackupCodes(userId: Long, remainingHashes: List<String>)
    suspend fun delete(userId: Long)
}
