@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.sections.HeroSection
import dev.kodex.webapp.pages.landing.sections.StatCounter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

class HeroSectionTest : FunSpec({
    context("StatCounter") {
        test("renders shimmer element when value is null and not error") {
            if (!isJsTarget()) return@test
            val host = renderComponent { StatCounter(label = PROBLEMS, value = null, error = false) }
            host.querySelector(STAT_SHIMMER).shouldNotBeNull()
            cleanupHost(host)
        }

        test("shimmer is not shown when value is provided") {
            if (!isJsTarget()) return@test
            val host = renderComponent { StatCounter(label = PROBLEMS, value = 42, error = false) }
            host.querySelector(STAT_SHIMMER) shouldBe null
            cleanupHost(host)
        }

        test("renders numeric text when value is provided") {
            if (!isJsTarget()) return@test
            val host = renderComponent { StatCounter(label = PROBLEMS, value = 1247, error = false) }
            val valueEl = host.querySelector(STAT_VALUE).shouldNotBeNull()
            valueEl.textContent shouldContain "1247"
            cleanupHost(host)
        }

        test("renders dash when error is true") {
            if (!isJsTarget()) return@test
            val host = renderComponent { StatCounter(label = PROBLEMS, value = null, error = true) }
            val valueEl = host.querySelector(STAT_VALUE).shouldNotBeNull()
            valueEl.textContent shouldContain "—"
            cleanupHost(host)
        }

        test("renders label text") {
            if (!isJsTarget()) return@test
            val host = renderComponent { StatCounter(label = "developers", value = 99) }
            val labelEl = host.querySelector(".stat-label").shouldNotBeNull()
            labelEl.textContent shouldContain "developers"
            cleanupHost(host)
        }
    }

    context("HeroSection editor panel") {
        test("mac window bar shows three dots") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            val dots = host.querySelectorAll(".mac-dot")
            dots?.length shouldBe 3

            cleanupHost(host)
        }

        test("editor panel container is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            host.querySelector("#hero-editor-panel").shouldNotBeNull()

            cleanupHost(host)
        }
    }

    context("HeroSection stats state") {
        test("shows shimmer counters when Loading") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            val shimmers = host.querySelectorAll(STAT_SHIMMER)
            shimmers?.length shouldBe 3

            cleanupHost(host)
        }

        test("shows dash counters when Error") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Error()) }

            val dashes = host.querySelectorAll(STAT_VALUE)
            dashes?.length shouldBe 3

            cleanupHost(host)
        }
    }

    context("HeroSection stats Success state") {
        test("displays loaded stats after counter animation") {
            if (!isJsTarget()) return@test
            val stats = LandingStats(totalProblems = 100, totalUsers = 200, totalContests = 10)
            val host = renderComponent { HeroSection(statsState = UiState.Success(stats)) }

            // Counter roll-up takes ~1.8s (60 steps × 30ms)
            delay(2200.milliseconds)

            val values = host.querySelectorAll(STAT_VALUE)
            values.shouldNotBeNull()
            values.length shouldBe 3

            cleanupHost(host)
        }
    }
})

private const val STAT_SHIMMER = ".stat-shimmer"
private const val PROBLEMS = "problems"
private const val STAT_VALUE = ".stat-value"
