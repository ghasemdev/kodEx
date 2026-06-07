@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.shared.session.Plan
import dev.kodex.shared.session.SessionState
import dev.kodex.shared.session.UserRole
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.pages.landing.sections.CreateExamSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CreateExamSectionTest : FunSpec({
    context("section root") {
        test("create-exam-section root element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection() }
            host.querySelector("#create-exam-section").shouldNotBeNull()
            cleanupHost(host)
        }

        test("heading is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection() }
            host.textContent shouldContain "Run Your Own Kotlin Exam"
            cleanupHost(host)
        }

        test("'For Educators' label is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection() }
            host.textContent shouldContain "For Educators"
            cleanupHost(host)
        }
    }

    context("checklist items") {
        test("renders exactly 5 checklist items") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection() }
            val items = host.querySelectorAll("[id^='checklist-item-']")
            items?.length shouldBe 5
            cleanupHost(host)
        }

        test("checklist-item-0 is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection() }
            host.querySelector("#checklist-item-0").shouldNotBeNull()
            cleanupHost(host)
        }

        test("checklist-item-4 (last) is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection() }
            host.querySelector("#checklist-item-4").shouldNotBeNull()
            cleanupHost(host)
        }
    }

    context("CTA button labels based on session") {
        test("shows 'Sign Up to Create' for Guest session") {
            if (!isJsTarget()) return@test
            val host = renderComponent { CreateExamSection(session = SessionState.Guest) }
            host.textContent shouldContain "Sign Up to Create"
            cleanupHost(host)
        }

        test("shows 'Create an Exam' for EXAM_CREATOR session") {
            if (!isJsTarget()) return@test
            val creator = SessionState.Authenticated(
                userId = "u1",
                username = "bob",
                avatarUrl = null,
                plan = Plan.PRO,
                roles = setOf(UserRole.EXAM_CREATOR),
            )
            val host = renderComponent { CreateExamSection(session = creator) }
            host.textContent shouldContain "Create an Exam"
            cleanupHost(host)
        }

        test("shows 'Create an Exam' for ADMIN session") {
            if (!isJsTarget()) return@test
            val admin = SessionState.Authenticated(
                userId = "u2",
                username = "carol",
                avatarUrl = null,
                plan = Plan.PRO,
                roles = setOf(UserRole.ADMIN),
            )
            val host = renderComponent { CreateExamSection(session = admin) }
            host.textContent shouldContain "Create an Exam"
            cleanupHost(host)
        }

        test("shows 'Sign Up to Create' for PARTICIPANT session") {
            if (!isJsTarget()) return@test
            val participant = SessionState.Authenticated(
                userId = "u3",
                username = "dave",
                avatarUrl = null,
                plan = Plan.FREE,
                roles = setOf(UserRole.PARTICIPANT),
            )
            val host = renderComponent { CreateExamSection(session = participant) }
            host.textContent shouldContain "Sign Up to Create"
            cleanupHost(host)
        }
    }
})
