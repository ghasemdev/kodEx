package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

object UserProfilesTable : LongIdTable("user_profiles") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val displayName = varchar("display_name", 100).nullable()
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val birthdate = date("birthdate").nullable()
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val location = varchar("location", 100).nullable()
    val githubUrl = varchar("github_url", 512).nullable()
    val linkedinUrl = varchar("linkedin_url", 512).nullable()
    val twitterUrl = varchar("twitter_url", 512).nullable()
    val websiteUrl = varchar("website_url", 512).nullable()
    val updatedAt = timestamp("updated_at")
}
