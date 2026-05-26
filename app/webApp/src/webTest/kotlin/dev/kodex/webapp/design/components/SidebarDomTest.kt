@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kilua.html.div
import dev.kodex.webapp.design.breakpoint.BreakpointTier
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.mouseEvent
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.w3c.dom.get

class SidebarDomTest : FunSpec({
    val items = listOf(
        NavItem(key = "home", label = "Home"),
        NavItem(key = "settings", label = "Settings")
    )

    test("renders sidebar container on non-mobile") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(items = items, breakpointProvider = { BreakpointTier.Desktop })
        }

        host.querySelector("aside").shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders navigation items") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(items = items, breakpointProvider = { BreakpointTier.Desktop })
        }

        val buttons = host.querySelectorAll("[role='button']")
        buttons?.length?.shouldBeGreaterThan(0)

        cleanupHost(host)
    }

    test("applies selected aria-current") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(
                items = items,
                selectedItem = "home",
                breakpointProvider = { BreakpointTier.Desktop },
            )
        }

        host.querySelector("[aria-current='page']").shouldNotBeNull()

        cleanupHost(host)
    }

    test("calls onItemSelect when item clicked") {
        if (!isJsTarget()) return@test

        var selected: String? = null

        val host = renderComponent {
            Sidebar(
                items = items,
                onItemSelect = { selected = it },
                breakpointProvider = { BreakpointTier.Desktop },
            )
        }

        val first = host.querySelectorAll("[role='button']")?.get(0)

        first?.dispatchEvent(mouseEvent())

        selected shouldBe "home"

        cleanupHost(host)
    }

    test("renders header when provided and not collapsed") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(
                items = items,
                header = {
                    div { +"Header" }
                },
                breakpointProvider = { BreakpointTier.Desktop },
            )
        }

        val text = host.textContent ?: ""
        text shouldContain "Header"

        cleanupHost(host)
    }

    test("renders footer when provided and not collapsed") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(
                items = items,
                footer = {
                    div { +"Footer" }
                },
                breakpointProvider = { BreakpointTier.Desktop },
            )
        }

        val text = host.textContent ?: ""
        text shouldContain "Footer"

        cleanupHost(host)
    }

    test("items contain labels") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(items = items, breakpointProvider = { BreakpointTier.Desktop })
        }

        val text = host.textContent ?: ""

        text shouldContain "Home"
        text shouldContain "Settings"

        cleanupHost(host)
    }

    test("collapse toggle exists on tablet layout") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(items = items, breakpointProvider = { BreakpointTier.Desktop })
        }

        val toggle = host.querySelector("span")
        toggle shouldNotBe null

        cleanupHost(host)
    }

    test("applies custom className") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Sidebar(
                items = items,
                className = "my-sidebar",
                breakpointProvider = { BreakpointTier.Desktop },
            )
        }

        val aside = host.querySelector("aside").shouldNotBeNull()
        aside.className shouldContain "my-sidebar"

        cleanupHost(host)
    }
})
