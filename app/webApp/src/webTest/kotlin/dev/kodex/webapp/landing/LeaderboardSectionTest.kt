@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.shared.session.SessionState
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.model.PlaceholderLeaderboard
import dev.kodex.webapp.pages.landing.sections.LeaderboardSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class LeaderboardSectionTest : FunSpec({
    context("section root") {
        test("leaderboard-section root element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection() }
            host.querySelector("#leaderboard-section").shouldNotBeNull()
            cleanupHost(host)
        }

        test("heading 'Leaderboard' is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection() }
            host.textContent shouldContain "Leaderboard"
            cleanupHost(host)
        }
    }

    context("leaderboard rows") {
        test("renders exactly ${PlaceholderLeaderboard.entries.size} rows") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection() }
            val rows = host.querySelectorAll("[id^='leaderboard-row-']")
            rows?.length shouldBe PlaceholderLeaderboard.entries.size
            cleanupHost(host)
        }

        test("row-0 element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection() }
            host.querySelector("#leaderboard-row-0").shouldNotBeNull()
            cleanupHost(host)
        }

        test("score-0 element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection() }
            host.querySelector("#leaderboard-score-0").shouldNotBeNull()
            cleanupHost(host)
        }

        test("first entry username is shown in row text") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection() }
            host.querySelector("#leaderboard-row-0")?.textContent shouldContain
                PlaceholderLeaderboard.entries.first().username
            cleanupHost(host)
        }
    }

    context("guest placeholder") {
        test("guest rank placeholder is shown for Guest session") {
            if (!isJsTarget()) return@test
            val host = renderComponent { LeaderboardSection(session = SessionState.Guest) }
            host.textContent shouldContain "Sign up to claim your rank"
            cleanupHost(host)
        }

        test("guest rank placeholder is NOT shown for Authenticated session") {
            if (!isJsTarget()) return@test
            val host = renderComponent {
                LeaderboardSection(
                    session = SessionState.Authenticated(
                        userId = "u1",
                        username = "alice",
                        avatarUrl = null,
                        plan = dev.kodex.shared.session.Plan.FREE,
                        roles = setOf(dev.kodex.shared.session.UserRole.PARTICIPANT),
                    ),
                )
            }
            host.querySelector("[class*='border-dashed']").shouldBeNull()
            cleanupHost(host)
        }
    }
})
