package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

class InputDomTest : FunSpec({
    test("renders input element") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input()
        }

        host.querySelector("input").shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders label when provided") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(label = "Username")
        }

        val label = host.querySelector("label").shouldNotBeNull()
        label.textContent.shouldNotBeNull() shouldContain "Username"

        cleanupHost(host)
    }

    test("sets placeholder on input") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(placeholder = "Enter name")
        }

        val input = host.querySelector("input").shouldNotBeNull()
        input.getAttribute("placeholder") shouldContain "Enter name"

        cleanupHost(host)
    }

    test("disabled=true sets disabled attribute") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(disabled = true)
        }

        val input = host.querySelector("input").shouldNotBeNull()
        input.hasAttribute("disabled").shouldBeTrue()

        cleanupHost(host)
    }

    test("renders helper text when no error") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(helperText = "Help text")
        }

        val p = host.querySelector("p").shouldNotBeNull()
        p.textContent.shouldNotBeNull() shouldContain "Help text"

        cleanupHost(host)
    }

    test("renders error instead of helper text") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(
                helperText = "Help text",
                error = "Something went wrong"
            )
        }

        val p = host.querySelector("p").shouldNotBeNull()
        p.textContent.shouldNotBeNull() shouldContain "Something went wrong"

        cleanupHost(host)
    }

    test("required shows asterisk in label") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(label = "Email", required = true)
        }

        val label = host.querySelector("label").shouldNotBeNull()
        label.textContent.shouldNotBeNull() shouldContain "*"

        cleanupHost(host)
    }

    test("applies custom className on root") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Input(className = "my-input")
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "my-input"

        cleanupHost(host)
    }
})
