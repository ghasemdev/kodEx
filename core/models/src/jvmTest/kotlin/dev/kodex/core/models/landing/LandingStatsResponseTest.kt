package dev.kodex.core.models.landing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LandingStatsResponseTest : FunSpec({
    test("serialization round trip") {
        val original = LandingStatsResponse(
            totalProblems = 1247,
            totalUsers = 8432,
            totalContests = 342,
        )
        val json = Json.encodeToString(LandingStatsResponse.serializer(), original)
        val decoded = Json.decodeFromString<LandingStatsResponse>(json)
        decoded shouldBe original
    }

    test("fields map correctly") {
        val response = LandingStatsResponse(
            totalProblems = 10,
            totalUsers = 20,
            totalContests = 5,
        )
        response.totalProblems shouldBe 10
        response.totalUsers shouldBe 20
        response.totalContests shouldBe 5
    }

    test("deserialization from JSON string") {
        val json = """{"totalProblems":1247,"totalUsers":8432,"totalContests":342}"""
        val response = Json.decodeFromString<LandingStatsResponse>(json)
        response.totalProblems shouldBe 1247
        response.totalUsers shouldBe 8432
        response.totalContests shouldBe 342
    }
})
