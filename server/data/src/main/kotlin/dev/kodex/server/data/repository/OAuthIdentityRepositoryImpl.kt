package dev.kodex.server.data.repository

import dev.kodex.core.models.auth.OAuthProvider
import dev.kodex.server.data.db.tables.OAuthIdentitiesTable
import dev.kodex.server.domain.auth.model.OAuthIdentityRecord
import dev.kodex.server.domain.auth.repository.OAuthIdentityRepository
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single(binds = [OAuthIdentityRepository::class])
class OAuthIdentityRepositoryImpl : OAuthIdentityRepository {
    override suspend fun findByProvider(provider: OAuthProvider, providerUserId: String): OAuthIdentityRecord? =
        suspendTransaction {
            OAuthIdentitiesTable.selectAll()
                .where {
                    (OAuthIdentitiesTable.provider eq provider) and
                        (OAuthIdentitiesTable.providerUserId eq providerUserId)
                }
                .firstOrNull()
                ?.toRecord()
        }

    override suspend fun findByUserId(userId: Long): List<OAuthIdentityRecord> = suspendTransaction {
        OAuthIdentitiesTable.selectAll()
            .where { OAuthIdentitiesTable.userId eq userId }
            .map { it.toRecord() }
    }

    override suspend fun create(userId: Long, provider: OAuthProvider, providerUserId: String): OAuthIdentityRecord =
        suspendTransaction {
            val id = OAuthIdentitiesTable.insertAndGetId {
                it[OAuthIdentitiesTable.userId] = userId
                it[OAuthIdentitiesTable.provider] = provider
                it[OAuthIdentitiesTable.providerUserId] = providerUserId
                it[linkedAt] = Clock.System.now()
            }
            OAuthIdentitiesTable.selectAll().where { OAuthIdentitiesTable.id eq id }.first().toRecord()
        }

    override suspend fun delete(userId: Long, provider: OAuthProvider) {
        suspendTransaction {
            OAuthIdentitiesTable.deleteWhere {
                (OAuthIdentitiesTable.userId eq userId) and (OAuthIdentitiesTable.provider eq provider)
            }
        }
    }

    override suspend fun countForUser(userId: Long): Int = suspendTransaction {
        OAuthIdentitiesTable.selectAll().where { OAuthIdentitiesTable.userId eq userId }.count().toInt()
    }

    private fun ResultRow.toRecord() = OAuthIdentityRecord(
        id = this[OAuthIdentitiesTable.id].value,
        userId = this[OAuthIdentitiesTable.userId].value,
        provider = this[OAuthIdentitiesTable.provider],
        providerUserId = this[OAuthIdentitiesTable.providerUserId],
        linkedAt = this[OAuthIdentitiesTable.linkedAt],
    )
}
