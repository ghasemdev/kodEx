@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.model.Badges
import dev.kodex.webapp.pages.landing.sections.GamificationSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank

class GamificationSectionTest : FunSpec({
    context("section root") {
        test("gamification-section root element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            host.querySelector("#gamification-section").shouldNotBeNull()
            cleanupHost(host)
        }

        test("heading 'Earn Your Rank' is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            host.textContent shouldContain "Earn Your Rank"
            cleanupHost(host)
        }
    }

    context("tier bar") {
        test("tier fill element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            host.querySelector("#gamification-tier-fill").shouldNotBeNull()
            cleanupHost(host)
        }
    }

    context("badge cards") {
        test("renders exactly ${Badges.all.size} badge cards") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            val cards = host.querySelectorAll(".badge-card")
            cards?.length shouldBe Badges.all.size
            cleanupHost(host)
        }

        test("each badge card has its expected DOM id") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            Badges.all.forEach { badge ->
                host.querySelector("#badge-card-${badge.id}").shouldNotBeNull()
            }
            cleanupHost(host)
        }

        test("each badge card has a non-blank aria-label") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            Badges.all.forEach { badge ->
                val card = host.querySelector("#badge-card-${badge.id}")
                card.shouldNotBeNull()
                card.getAttribute("aria-label").shouldNotBeBlank()
            }
            cleanupHost(host)
        }

        test("first badge card aria-label contains badge name") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            val firstBadge = Badges.all.first()
            val card = host.querySelector("#badge-card-${firstBadge.id}")
            card.shouldNotBeNull()
            card.getAttribute("aria-label") shouldContain firstBadge.name
            cleanupHost(host)
        }

        test("badge cards have tabindex 0") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            val firstBadge = Badges.all.first()
            val card = host.querySelector("#badge-card-${firstBadge.id}")
            card.shouldNotBeNull()
            card.getAttribute("tabindex") shouldBe "0"
            cleanupHost(host)
        }

        test("badge cards have role button") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GamificationSection() }
            val firstBadge = Badges.all.first()
            val card = host.querySelector("#badge-card-${firstBadge.id}")
            card.shouldNotBeNull()
            card.getAttribute("role") shouldBe "button"
            cleanupHost(host)
        }
    }
})
