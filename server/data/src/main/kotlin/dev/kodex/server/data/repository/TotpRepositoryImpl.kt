package dev.kodex.server.data.repository

import dev.kodex.server.data.db.tables.TotpConfigsTable
import dev.kodex.server.domain.totp.repository.TotpRecord
import dev.kodex.server.domain.totp.repository.TotpRepository
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update

class TotpRepositoryImpl : TotpRepository {
    override suspend fun findByUserId(userId: Long): TotpRecord? = suspendTransaction {
        TotpConfigsTable.selectAll().where { TotpConfigsTable.userId eq userId }.firstOrNull()?.toRecord()
    }

    override suspend fun create(userId: Long, secretEncrypted: String): TotpRecord = suspendTransaction {
        val id = TotpConfigsTable.insertAndGetId {
            it[TotpConfigsTable.userId] = userId
            it[TotpConfigsTable.secretEncrypted] = secretEncrypted
            it[updatedAt] = Clock.System.now()
        }
        TotpConfigsTable.selectAll().where { TotpConfigsTable.id eq id }.first().toRecord()
    }

    override suspend fun enable(userId: Long, backupCodesHashes: List<String>) {
        suspendTransaction {
            TotpConfigsTable.update({ TotpConfigsTable.userId eq userId }) {
                it[enabled] = true
                it[TotpConfigsTable.backupCodesHashes] = Json.encodeToString(backupCodesHashes)
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun updateBackupCodes(userId: Long, remainingHashes: List<String>) {
        suspendTransaction {
            TotpConfigsTable.update({ TotpConfigsTable.userId eq userId }) {
                it[backupCodesHashes] = Json.encodeToString(remainingHashes)
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun delete(userId: Long) {
        suspendTransaction {
            TotpConfigsTable.deleteWhere { TotpConfigsTable.userId eq userId }
        }
    }

    private fun ResultRow.toRecord() = TotpRecord(
        id = this[TotpConfigsTable.id].value,
        userId = this[TotpConfigsTable.userId].value,
        secretEncrypted = this[TotpConfigsTable.secretEncrypted],
        enabled = this[TotpConfigsTable.enabled],
        backupCodesHashes = Json.decodeFromString<List<String>>(this[TotpConfigsTable.backupCodesHashes]),
        updatedAt = this[TotpConfigsTable.updatedAt],
    )
}
