@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.model.PlaceholderProblems
import dev.kodex.webapp.pages.landing.sections.ProblemsSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ProblemsSectionTest : FunSpec({
    context("section root") {
        test("problems-section root element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            host.querySelector("#problems-section").shouldNotBeNull()
            cleanupHost(host)
        }

        test("heading 'Problem Dataset' is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            host.textContent shouldContain "Problem Dataset"
            cleanupHost(host)
        }
    }

    context("problem rows") {
        test("renders exactly ${PlaceholderProblems.entries.size} problem rows") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            val rows = host.querySelectorAll("[id^='problem-row-']")
            rows?.length shouldBe PlaceholderProblems.entries.size
            cleanupHost(host)
        }

        test("problem-row-0 is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            host.querySelector("#problem-row-0").shouldNotBeNull()
            cleanupHost(host)
        }

        test("first problem title is rendered in row-0") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            host.querySelector("#problem-row-0")?.textContent shouldContain
                PlaceholderProblems.entries.first().title
            cleanupHost(host)
        }

        test("last problem row id is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            val lastIdx = PlaceholderProblems.entries.lastIndex
            host.querySelector("#problem-row-$lastIdx").shouldNotBeNull()
            cleanupHost(host)
        }

        test("problem rows have role button") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            val row = host.querySelector("#problem-row-0")
            row.shouldNotBeNull()
            row.getAttribute("role") shouldBe "button"
            cleanupHost(host)
        }

        test("problem rows have tabindex 0") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            val row = host.querySelector("#problem-row-0")
            row.shouldNotBeNull()
            row.getAttribute("tabindex") shouldBe "0"
            cleanupHost(host)
        }
    }

    context("filter chips") {
        test("'Explore all problems' link is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            host.textContent shouldContain "Explore all problems"
            cleanupHost(host)
        }

        test("filter chip 'All' is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ProblemsSection() }
            host.textContent shouldContain "All"
            cleanupHost(host)
        }
    }
})
