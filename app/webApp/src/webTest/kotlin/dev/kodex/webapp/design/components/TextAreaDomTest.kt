@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class TextAreaDomTest : FunSpec({
    test("renders textarea element") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea()
        }

        host.querySelector(QUERY_SELECTOR).shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders label when provided") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(label = "Description")
        }

        val label = host.querySelector("label").shouldNotBeNull()
        label.textContent.shouldNotBeNull() shouldContain "Description"

        cleanupHost(host)
    }

    test("applies placeholder to textarea") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(placeholder = "Write here...")
        }

        val node = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        node.getAttribute("placeholder") shouldContain "Write here..."

        cleanupHost(host)
    }

    test("sets rows attribute") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(rows = 6)
        }

        val node = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        node.getAttribute("rows") shouldContain "6"

        cleanupHost(host)
    }

    test("disabled sets attribute") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(disabled = true)
        }

        val node = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        node.hasAttribute("disabled").shouldBeTrue()

        cleanupHost(host)
    }

    test("renders helper text when no error") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(helperText = HELP_TEXT)
        }

        val text = host.textContent ?: ""
        text shouldContain HELP_TEXT

        cleanupHost(host)
    }

    test("renders error instead of helper text") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(
                helperText = HELP_TEXT,
                error = "Something went wrong",
            )
        }

        val text = host.textContent ?: ""
        text shouldContain "Something went wrong"

        cleanupHost(host)
    }

    test("required shows asterisk") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(label = "Bio", required = true)
        }

        val label = host.querySelector("label").shouldNotBeNull()
        label.textContent.shouldNotBeNull() shouldContain "*"

        cleanupHost(host)
    }

    test("renders counter when enabled") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(
                value = "Hello",
                maxLength = 100,
                showCounter = true,
            )
        }

        val text = host.textContent ?: ""
        text shouldContain "5/100"

        cleanupHost(host)
    }

    test("does not render counter when disabled") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(
                value = "Hello",
                maxLength = 100,
                showCounter = false,
            )
        }

        val text = host.textContent ?: ""

        text shouldNotContain "5/100"

        cleanupHost(host)
    }

    test("custom className is applied") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            TextArea(className = "my-textarea")
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "my-textarea"

        cleanupHost(host)
    }
})

private const val QUERY_SELECTOR = "textarea"
private const val HELP_TEXT = "Help text"
