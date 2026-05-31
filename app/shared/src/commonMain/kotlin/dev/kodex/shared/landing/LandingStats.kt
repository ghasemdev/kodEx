package dev.kodex.shared.landing

import androidx.compose.runtime.Immutable

@Immutable
data class LandingStats(
    val totalProblems: Int,
    val totalUsers: Int,
    val totalContests: Int,
)
