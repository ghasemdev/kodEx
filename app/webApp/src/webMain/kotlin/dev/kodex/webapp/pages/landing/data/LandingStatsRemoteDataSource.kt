package dev.kodex.webapp.pages.landing.data

import dev.kodex.core.models.landing.LandingStatsResponse

interface LandingStatsRemoteDataSource {
    suspend fun fetchStats(): LandingStatsResponse
}
