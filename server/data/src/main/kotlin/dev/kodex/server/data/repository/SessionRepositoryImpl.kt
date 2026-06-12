package dev.kodex.server.data.repository

import dev.kodex.core.models.auth.SessionDto
import dev.kodex.server.data.db.tables.RefreshTokensTable
import dev.kodex.server.domain.users.repository.SessionRepository
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single(binds = [SessionRepository::class])
class SessionRepositoryImpl : SessionRepository {
    override suspend fun findActiveByUserId(userId: Long, currentTokenHash: String?): List<SessionDto> =
        suspendTransaction {
            val now = Clock.System.now()
            RefreshTokensTable.selectAll()
                .where {
                    (RefreshTokensTable.userId eq userId) and
                        RefreshTokensTable.revokedAt.isNull() and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .orderBy(RefreshTokensTable.issuedAt, SortOrder.DESC)
                .map { row ->
                    SessionDto(
                        id = row[RefreshTokensTable.id].value,
                        deviceHint = row[RefreshTokensTable.deviceHint],
                        ipAddress = row[RefreshTokensTable.ipAddress],
                        issuedAt = row[RefreshTokensTable.issuedAt],
                        expiresAt = row[RefreshTokensTable.expiresAt],
                        isCurrent = currentTokenHash != null && row[RefreshTokensTable.tokenHash] == currentTokenHash,
                    )
                }
        }

    override suspend fun revokeById(sessionId: Long, userId: Long) {
        suspendTransaction {
            RefreshTokensTable.update({
                (RefreshTokensTable.id eq sessionId) and (RefreshTokensTable.userId eq userId)
            }) {
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun revokeAllExceptCurrent(userId: Long, currentTokenHash: String) {
        suspendTransaction {
            RefreshTokensTable.update({
                (RefreshTokensTable.userId eq userId) and
                    (RefreshTokensTable.tokenHash neq currentTokenHash) and
                    RefreshTokensTable.revokedAt.isNull()
            }) {
                it[revokedAt] = Clock.System.now()
            }
        }
    }
}
