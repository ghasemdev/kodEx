package dev.kodex.webapp.landing

import dev.kodex.core.models.landing.LandingStatsResponse
import dev.kodex.shared.landing.LandingStats
import dev.kodex.webapp.pages.landing.data.LandingStatsRemoteDataSource
import dev.kodex.webapp.pages.landing.data.LandingStatsRepositoryImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LandingStatsRepositoryImplTest : FunSpec({
    test("fetchStats maps LandingStatsResponse to LandingStats domain model") {
        val fixture = LandingStatsResponse(
            totalProblems = 42,
            totalUsers = 100,
            totalContests = 7,
        )
        val fakeRemoteDataSource = object : LandingStatsRemoteDataSource {
            override suspend fun fetchStats(): LandingStatsResponse = fixture
        }
        val repo = LandingStatsRepositoryImpl(fakeRemoteDataSource)
        val result = repo.fetchStats()
        result shouldBe LandingStats(totalProblems = 42, totalUsers = 100, totalContests = 7)
    }

    test("fetchStats preserves all three fields independently") {
        val fixture = LandingStatsResponse(
            totalProblems = TOTAL_PROBLEMS,
            totalUsers = TOTAL_USERS,
            totalContests = TOTAL_CONTESTS,
        )
        val fakeRemoteDataSource = object : LandingStatsRemoteDataSource {
            override suspend fun fetchStats(): LandingStatsResponse = fixture
        }
        val repo = LandingStatsRepositoryImpl(fakeRemoteDataSource)
        val result = repo.fetchStats()

        result.totalProblems shouldBe TOTAL_PROBLEMS
        result.totalUsers shouldBe TOTAL_USERS
        result.totalContests shouldBe TOTAL_CONTESTS
    }

    test("fetchStats delegates to remoteDataSource each call") {
        var callCount = 0
        val fixture = LandingStatsResponse(totalProblems = 1, totalUsers = 2, totalContests = 3)
        val fakeRemoteDataSource = object : LandingStatsRemoteDataSource {
            override suspend fun fetchStats(): LandingStatsResponse {
                callCount++
                return fixture
            }
        }
        val repo = LandingStatsRepositoryImpl(fakeRemoteDataSource)
        repo.fetchStats()
        repo.fetchStats()
        callCount shouldBe 2
    }

    test("fetchStats with zero values maps correctly") {
        val fixture = LandingStatsResponse(totalProblems = 0, totalUsers = 0, totalContests = 0)
        val fakeRemoteDataSource = object : LandingStatsRemoteDataSource {
            override suspend fun fetchStats(): LandingStatsResponse = fixture
        }
        val repo = LandingStatsRepositoryImpl(fakeRemoteDataSource)
        val result = repo.fetchStats()
        result shouldBe LandingStats(totalProblems = 0, totalUsers = 0, totalContests = 0)
    }
})

private const val TOTAL_PROBLEMS = 1247
private const val TOTAL_USERS = 8432
private const val TOTAL_CONTESTS = 342
