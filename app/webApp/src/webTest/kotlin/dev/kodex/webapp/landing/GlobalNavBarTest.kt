@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.shared.session.Plan
import dev.kodex.shared.session.SessionState
import dev.kodex.shared.session.UserRole
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.layout.GlobalNavBar
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

private const val NAV_CREATE_EXAM = "#nav-create-exam"

class GlobalNavBarTest : FunSpec({
    context("Guest state") {
        test("Sign In button is visible for guest") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = SessionState.Guest) }

            host.querySelector("#nav-sign-in").shouldNotBeNull()

            cleanupHost(host)
        }

        test("Sign Up button is visible for guest") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = SessionState.Guest) }

            host.querySelector("#nav-sign-up").shouldNotBeNull()

            cleanupHost(host)
        }

        test("avatar is not shown for guest") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = SessionState.Guest) }

            host.querySelector("#nav-avatar").shouldBeNull()

            cleanupHost(host)
        }

        test("Create Exam link is not shown for guest") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = SessionState.Guest) }

            host.querySelector(NAV_CREATE_EXAM).shouldBeNull()

            cleanupHost(host)
        }
    }

    context("Authenticated PARTICIPANT state") {
        val participant = SessionState.Authenticated(
            userId = "user-1",
            username = "alice",
            avatarUrl = null,
            plan = Plan.FREE,
            roles = setOf(UserRole.PARTICIPANT),
        )

        test("avatar is shown for authenticated user") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = participant) }

            host.querySelector("#nav-avatar").shouldNotBeNull()

            cleanupHost(host)
        }

        test("username is shown for authenticated user") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = participant) }

            host.querySelector("#nav-username").shouldNotBeNull()

            cleanupHost(host)
        }

        test("plan badge is shown for authenticated user") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = participant) }

            host.querySelector("#nav-plan-badge").shouldNotBeNull()

            cleanupHost(host)
        }

        test("Sign In is not shown for authenticated user") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = participant) }

            host.querySelector("#nav-sign-in").shouldBeNull()

            cleanupHost(host)
        }

        test("Create Exam is not shown for PARTICIPANT role") {
            if (!isJsTarget()) return@test
            val host = renderComponent { GlobalNavBar(session = participant) }

            host.querySelector(NAV_CREATE_EXAM).shouldBeNull()

            cleanupHost(host)
        }
    }

    context("EXAM_CREATOR role") {
        test("Create Exam is shown for EXAM_CREATOR role") {
            if (!isJsTarget()) return@test
            val creator = SessionState.Authenticated(
                userId = "user-2",
                username = "bob",
                avatarUrl = null,
                plan = Plan.PRO,
                roles = setOf(UserRole.EXAM_CREATOR),
            )
            val host = renderComponent { GlobalNavBar(session = creator) }

            host.querySelector(NAV_CREATE_EXAM).shouldNotBeNull()

            cleanupHost(host)
        }

        test("Create Exam is shown for ADMIN role") {
            if (!isJsTarget()) return@test
            val admin = SessionState.Authenticated(
                userId = "user-3",
                username = "carol",
                avatarUrl = null,
                plan = Plan.PRO,
                roles = setOf(UserRole.ADMIN),
            )
            val host = renderComponent { GlobalNavBar(session = admin) }

            host.querySelector(NAV_CREATE_EXAM).shouldNotBeNull()

            cleanupHost(host)
        }
    }
})
