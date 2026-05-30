package dev.kodex.shared.landing

interface LandingStatsRepository {
    suspend fun fetchStats(): LandingStats
}
