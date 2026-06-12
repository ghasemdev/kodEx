package dev.kodex.server.domain.totp.model

import kotlin.time.Instant

data class TotpRecord(
    val id: Long,
    val userId: Long,
    val secretEncrypted: String,
    val enabled: Boolean,
    val backupCodesHashes: List<String>,
    val updatedAt: Instant,
)
