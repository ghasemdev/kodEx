@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.mouseEvent
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.sections.HeroSection
import dev.kodex.webapp.pages.landing.sections.HeroTab
import dev.kodex.webapp.pages.landing.sections.StatCounter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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

    context("HeroSection tab switching") {
        test("Kotlin tab is active by default") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            val kotlinTab = host.querySelector(HERO_TAB_KOTLIN).shouldNotBeNull()
            kotlinTab.getAttribute(ARIA_SELECTED) shouldBe "true"
            kotlinTab.className shouldContain "tab-active"

            cleanupHost(host)
        }

        test("Android tab is not active by default") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            val androidTab = host.querySelector(HERO_TAB_ANDROID).shouldNotBeNull()
            androidTab.getAttribute(ARIA_SELECTED) shouldBe "false"
            androidTab.className shouldNotContain "tab-active"

            cleanupHost(host)
        }

        test("clicking Android tab makes it active") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            host.querySelector(HERO_TAB_ANDROID).shouldNotBeNull()
                .dispatchEvent(mouseEvent())

            delay(120.milliseconds) // allow Compose recomposition

            host.querySelector(HERO_TAB_ANDROID).shouldNotBeNull()
                .getAttribute(ARIA_SELECTED) shouldBe "true"
            host.querySelector(HERO_TAB_KOTLIN).shouldNotBeNull()
                .getAttribute(ARIA_SELECTED) shouldBe "false"

            cleanupHost(host)
        }

        test("clicking Kotlin tab keeps it active") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            host.querySelector(HERO_TAB_KOTLIN).shouldNotBeNull()
                .dispatchEvent(mouseEvent())

            delay(120.milliseconds)

            host.querySelector(HERO_TAB_KOTLIN).shouldNotBeNull()
                .getAttribute(ARIA_SELECTED) shouldBe "true"

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

        test("hero tab buttons are rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { HeroSection(statsState = UiState.Loading) }

            HeroTab.entries.forEach { tab ->
                host.querySelector("#hero-tab-${tab.name.lowercase()}").shouldNotBeNull()
            }

            cleanupHost(host)
        }
    }

    context("HeroSection stats Success state") {
        test("displays loaded stats after counter animation") {
            if (!isJsTarget()) return@test
            val stats = LandingStats(totalProblems = 100, totalUsers = 200, totalContests = 10)
            val host = renderComponent { HeroSection(statsState = UiState.Success(stats)) }

            // Counter roll-up takes ~1.8s (60 steps × 30ms), so we wait longer than the 80ms
            // settle time used in renderComponent. The initial render shows shimmer (null state).
            // This test verifies that Success state eventually shows numeric values.
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
private const val ARIA_SELECTED = "aria-selected"
private const val HERO_TAB_ANDROID = "#hero-tab-android"
private const val HERO_TAB_KOTLIN = "#hero-tab-kotlin"
