package dev.kodex.core.models.landing

import kotlinx.serialization.Serializable

@Serializable
data class LandingStatsResponse(
    val totalProblems: Int,
    val totalUsers: Int,
    val totalContests: Int,
)
