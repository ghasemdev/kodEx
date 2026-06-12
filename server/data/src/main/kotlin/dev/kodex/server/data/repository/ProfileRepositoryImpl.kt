package dev.kodex.server.data.repository

import dev.kodex.server.data.db.tables.UserProfilesTable
import dev.kodex.server.domain.users.model.ProfileRecord
import dev.kodex.server.domain.users.repository.ProfileRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single(binds = [ProfileRepository::class])
class ProfileRepositoryImpl : ProfileRepository {
    override suspend fun findByUserId(userId: Long): ProfileRecord? = suspendTransaction {
        UserProfilesTable.selectAll().where { UserProfilesTable.userId eq userId }.firstOrNull()?.toRecord()
    }

    override suspend fun create(userId: Long) {
        suspendTransaction {
            UserProfilesTable.insertAndGetId {
                it[UserProfilesTable.userId] = userId
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun update(
        userId: Long,
        displayName: String?,
        firstName: String?,
        lastName: String?,
        birthdate: LocalDate?,
        location: String?,
        githubUrl: String?,
        linkedinUrl: String?,
        twitterUrl: String?,
        websiteUrl: String?,
    ) {
        suspendTransaction {
            UserProfilesTable.update({ UserProfilesTable.userId eq userId }) {
                it[UserProfilesTable.displayName] = displayName
                it[UserProfilesTable.firstName] = firstName
                it[UserProfilesTable.lastName] = lastName
                it[UserProfilesTable.birthdate] = birthdate
                it[UserProfilesTable.location] = location
                it[UserProfilesTable.githubUrl] = githubUrl
                it[UserProfilesTable.linkedinUrl] = linkedinUrl
                it[UserProfilesTable.twitterUrl] = twitterUrl
                it[UserProfilesTable.websiteUrl] = websiteUrl
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun updateAvatarUrl(userId: Long, avatarUrl: String) {
        suspendTransaction {
            UserProfilesTable.update({ UserProfilesTable.userId eq userId }) {
                it[UserProfilesTable.avatarUrl] = avatarUrl
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    private fun ResultRow.toRecord() = ProfileRecord(
        userId = this[UserProfilesTable.userId].value,
        displayName = this[UserProfilesTable.displayName],
        firstName = this[UserProfilesTable.firstName],
        lastName = this[UserProfilesTable.lastName],
        birthdate = this[UserProfilesTable.birthdate],
        avatarUrl = this[UserProfilesTable.avatarUrl],
        location = this[UserProfilesTable.location],
        githubUrl = this[UserProfilesTable.githubUrl],
        linkedinUrl = this[UserProfilesTable.linkedinUrl],
        twitterUrl = this[UserProfilesTable.twitterUrl],
        websiteUrl = this[UserProfilesTable.websiteUrl],
        updatedAt = this[UserProfilesTable.updatedAt],
    )
}
