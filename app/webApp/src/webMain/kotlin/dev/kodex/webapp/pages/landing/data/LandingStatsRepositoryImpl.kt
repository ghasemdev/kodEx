package dev.kodex.webapp.pages.landing.data

import dev.kodex.core.models.landing.LandingStatsResponse
import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.landing.LandingStatsRepository
import org.koin.core.annotation.Single

@Single(binds = [LandingStatsRepository::class])
class LandingStatsRepositoryImpl(
    private val remoteDataSource: LandingStatsRemoteDataSource,
) : LandingStatsRepository {
    override suspend fun fetchStats(): LandingStats = remoteDataSource.fetchStats().toDomainModel()

    // Future: inject a LocalDataSource and apply cache-or-remote logic here.
    // val cached = localDataSource.getCachedStats()
    // if (cached != null && !cached.isExpired()) return cached.data
    // return remoteDataSource.fetchStats().toDomainModel().also { localDataSource.save(it) }
}

private fun LandingStatsResponse.toDomainModel() = LandingStats(
    totalProblems = totalProblems,
    totalUsers = totalUsers,
    totalContests = totalContests,
)
