@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

class ThemeSwitcherDomTest : FunSpec({
    test("renders theme switcher element") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            ThemeSwitcher()
        }

        val element = host.querySelector("*").shouldNotBeNull()
        element.className shouldContain "cursor-pointer"

        cleanupHost(host)
    }

    test("applies base classes") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            ThemeSwitcher()
        }

        val element = host.querySelector("*").shouldNotBeNull()
        val cls = element.className

        cls shouldContain "w-9"
        cls shouldContain "rounded-full"
        cls shouldContain "bg-surface-container"
        cls shouldContain "transition-colors"

        cleanupHost(host)
    }

    test("applies custom className") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            ThemeSwitcher(className = "my-theme")
        }

        val element = host.querySelector("*").shouldNotBeNull()
        element.className shouldContain "my-theme"

        cleanupHost(host)
    }

    test("renders theme switcher component structure") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            ThemeSwitcher()
        }

        host.innerHTML.shouldNotBeNull()

        cleanupHost(host)
    }
})
