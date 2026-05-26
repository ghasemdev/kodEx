@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kilua.html.div
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.mouseEvent
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.w3c.dom.get

class NavBarDomTest : FunSpec({
    val items = listOf(
        NavItem(key = "home", label = "Home"),
        NavItem(key = "profile", label = "Profile"),
    )

    test("renders nav container") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(items = items)
        }

        host.querySelector("nav").shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders navigation items") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(items = items)
        }

        val buttons = host.querySelectorAll("[role='button']")
        buttons?.length?.shouldBeGreaterThan(0)

        cleanupHost(host)
    }

    test("applies selected item aria-current") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(
                items = items,
                selectedItem = "home",
            )
        }

        val selected = host.querySelector("[aria-current='page']")
        selected.shouldNotBeNull()

        cleanupHost(host)
    }

    test("calls onItemSelect when item clicked") {
        if (!isJsTarget()) return@test

        var selected: String? = null

        val host = renderComponent {
            NavBar(
                items = items,
                onItemSelect = { selected = it },
            )
        }

        val first = host.querySelectorAll("[role='button']")?.get(0)
        first?.dispatchEvent(mouseEvent())

        selected shouldBe "home"

        cleanupHost(host)
    }

    test("renders actions slot on desktop layout") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(
                items = items,
                actions = {
                    div { +"Action" }
                },
            )
        }

        val nav = host.querySelector("nav").shouldNotBeNull()
        nav.textContent shouldContain "Action"

        cleanupHost(host)
    }

    test("has mobile structure nav bar exists") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(items = items)
        }

        host.querySelector("nav").shouldNotBeNull()

        cleanupHost(host)
    }

    test("item has correct text content") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(items = items)
        }

        val nav = host.querySelector("nav").shouldNotBeNull()
        val text = nav.textContent ?: ""

        text shouldContain "Home"
        text shouldContain "Profile"

        cleanupHost(host)
    }

    test("custom className is applied") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            NavBar(
                items = items,
                className = "my-navbar",
            )
        }

        val nav = host.querySelector("nav").shouldNotBeNull()
        nav.className shouldContain "my-navbar"

        cleanupHost(host)
    }
})
