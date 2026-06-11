package dev.kodex.server.data.repository

import dev.kodex.server.data.db.tables.EmergencyRevokeTokensTable
import dev.kodex.server.domain.auth.repository.EmergencyRevokeTokenRecord
import dev.kodex.server.domain.auth.repository.EmergencyRevokeTokenRepository
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update

class EmergencyRevokeTokenRepositoryImpl : EmergencyRevokeTokenRepository {
    override suspend fun create(userId: Long, tokenHash: String, expiresAt: Instant): EmergencyRevokeTokenRecord =
        suspendTransaction {
            val id = EmergencyRevokeTokensTable.insertAndGetId {
                it[EmergencyRevokeTokensTable.userId] = userId
                it[EmergencyRevokeTokensTable.tokenHash] = tokenHash
                it[EmergencyRevokeTokensTable.expiresAt] = expiresAt
            }
            EmergencyRevokeTokensTable.selectAll()
                .where { EmergencyRevokeTokensTable.id eq id }
                .first()
                .toRecord()
        }

    override suspend fun findActiveByHash(tokenHash: String): EmergencyRevokeTokenRecord? = newSuspendedTransaction {
        val now = Clock.System.now()
        EmergencyRevokeTokensTable.selectAll()
            .where {
                (EmergencyRevokeTokensTable.tokenHash eq tokenHash) and
                    EmergencyRevokeTokensTable.usedAt.isNull() and
                    (EmergencyRevokeTokensTable.expiresAt greater now)
            }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun markUsed(id: Long) {
        suspendTransaction {
            EmergencyRevokeTokensTable.update({ EmergencyRevokeTokensTable.id eq id }) {
                it[usedAt] = Clock.System.now()
            }
        }
    }

    private fun ResultRow.toRecord() = EmergencyRevokeTokenRecord(
        id = this[EmergencyRevokeTokensTable.id].value,
        userId = this[EmergencyRevokeTokensTable.userId].value,
        tokenHash = this[EmergencyRevokeTokensTable.tokenHash],
        expiresAt = this[EmergencyRevokeTokensTable.expiresAt],
        usedAt = this[EmergencyRevokeTokensTable.usedAt],
    )
}
