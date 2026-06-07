@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.mouseEvent
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.w3c.dom.events.Event

@OptIn(ExperimentalWasmJsInterop::class)
class LanguageSwitcherDomTest : FunSpec({
    test("renders toggle button") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher()
        }

        val toggle = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        toggle.className shouldContain "cursor-pointer"

        cleanupHost(host)
    }

    test("shows current language label") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher()
        }

        val toggle = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        val text = toggle.textContent ?: ""

        text.isNotBlank().shouldBeTrue()

        cleanupHost(host)
    }

    test("dropdown is hidden by default") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher()
        }

        host.querySelector("[role='listbox']") shouldBe null

        cleanupHost(host)
    }

    test("dropdown opens when toggle is clicked") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher()
        }

        val toggle = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        toggle.dispatchEvent(mouseEvent())
        delay(80.milliseconds)
        val dropdown = host.querySelector("[role='listbox']")
        dropdown.shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders language options when open") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher()
        }

        val toggle = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        toggle.dispatchEvent(mouseEvent())
        delay(80.milliseconds)
        val options = host.querySelectorAll("[role='option']")
        options?.length?.shouldBeGreaterThan(0)

        cleanupHost(host)
    }

    test("dropdown has correct aria attributes") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher()
        }

        val toggle = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()

        toggle.getAttribute("aria-haspopup") shouldContain "listbox"

        cleanupHost(host)
    }

    test("applies custom className") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            LanguageSwitcher(className = "my-switcher")
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "my-switcher"

        cleanupHost(host)
    }
})

private const val QUERY_SELECTOR = "div[role='button']"
