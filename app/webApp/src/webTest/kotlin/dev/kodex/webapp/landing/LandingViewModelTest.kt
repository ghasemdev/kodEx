@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.landing.LandingStatsRepository
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.pages.landing.LandingViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

private val successStats = LandingStats(totalProblems = 42, totalUsers = 100, totalContests = 7)

class LandingViewModelTest : FunSpec({
    context("initial state") {
        test("uiState starts as Loading before fetch completes") {
            if (!isJsTarget()) return@test
            // FakeDelayedRepo suspends for 50ms so Loading is observable right after construction
            val vm = LandingViewModel(FakeDelayedRepo())
            vm.uiState.value.statsState.shouldBeInstanceOf<UiState.Loading>()
            vm.onCleared()
        }
    }

    context("success path") {
        test("uiState transitions to Success after fetch") {
            if (!isJsTarget()) return@test
            val vm = LandingViewModel(FakeSuccessRepo())
            delay(100.milliseconds)
            vm.uiState.value.statsState.shouldBeInstanceOf<UiState.Success<LandingStats>>()
            vm.onCleared()
        }

        test("Success state contains the fetched stats") {
            if (!isJsTarget()) return@test
            val vm = LandingViewModel(FakeSuccessRepo())
            delay(100.milliseconds)
            val state = vm.uiState.value.statsState
            state.shouldBeInstanceOf<UiState.Success<LandingStats>>()
            state.data shouldBe successStats
            vm.onCleared()
        }
    }

    context("error path") {
        test("uiState transitions to Error when repo throws") {
            if (!isJsTarget()) return@test
            val vm = LandingViewModel(FakeErrorRepo("network error"))
            delay(100.milliseconds)
            vm.uiState.value.statsState.shouldBeInstanceOf<UiState.Error>()
            vm.onCleared()
        }

        test("Error state contains the error message") {
            if (!isJsTarget()) return@test
            val vm = LandingViewModel(FakeErrorRepo("network error"))
            delay(100.milliseconds)
            val state = vm.uiState.value.statsState
            state.shouldBeInstanceOf<UiState.Error>()
            state.message shouldBe "network error"
            vm.onCleared()
        }

        test("Error state message is null when exception has no message") {
            if (!isJsTarget()) return@test
            val vm = LandingViewModel(FakeErrorRepo(null))
            delay(100.milliseconds)
            val state = vm.uiState.value.statsState
            state.shouldBeInstanceOf<UiState.Error>()
            state.message shouldBe null
            vm.onCleared()
        }
    }
})

private class FakeSuccessRepo : LandingStatsRepository {
    override suspend fun fetchStats(): LandingStats = successStats
}

private class FakeDelayedRepo : LandingStatsRepository {
    override suspend fun fetchStats(): LandingStats {
        delay(50.milliseconds)
        return successStats
    }
}

private class FakeErrorRepo(private val message: String?) : LandingStatsRepository {
    override suspend fun fetchStats(): LandingStats = throw RuntimeException(message)
}
