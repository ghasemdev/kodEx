@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.sections.ExamTypesSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ExamTypesSectionTest : FunSpec({
    context("section root") {
        test("section root element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-types-section").shouldNotBeNull()
            cleanupHost(host)
        }
    }

    context("exam type cards") {
        test("renders exactly 3 exam type cards") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            val cards = host.querySelectorAll("[id^='exam-type-card-']")
            cards?.length shouldBe 3
            cleanupHost(host)
        }

        test("card-0 is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-type-card-0").shouldNotBeNull()
            cleanupHost(host)
        }

        test("card-1 is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-type-card-1").shouldNotBeNull()
            cleanupHost(host)
        }

        test("card-2 is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-type-card-2").shouldNotBeNull()
            cleanupHost(host)
        }

        test("section heading text is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.textContent shouldContain "Three ways to challenge developers"
            cleanupHost(host)
        }

        test("Quiz card title renders Multiple Choice") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-type-card-0")?.textContent shouldContain "Multiple Choice"
            cleanupHost(host)
        }

        test("IO card title renders I/O Test Cases") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-type-card-1")?.textContent shouldContain "I/O Test Cases"
            cleanupHost(host)
        }

        test("Injection card title renders Injection / Project") {
            if (!isJsTarget()) return@test
            val host = renderComponent { ExamTypesSection() }
            host.querySelector("#exam-type-card-2")?.textContent shouldContain "Injection / Project"
            cleanupHost(host)
        }
    }
})
