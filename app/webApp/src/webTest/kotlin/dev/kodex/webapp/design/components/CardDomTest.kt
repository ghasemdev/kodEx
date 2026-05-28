@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kilua.html.div
import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

class CardDomTest : FunSpec({
    test("renders root card div") {
        if (!isJsTarget()) return@test
        val host = renderComponent {
            Card(content = { })
        }

        host.querySelector("div").shouldNotBeNull()
        cleanupHost(host)
    }

    test("renders content inside card") {
        if (!isJsTarget()) return@test
        val host = renderComponent {
            Card {
                div { +"Hello Card" }
            }
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.textContent.shouldNotBeNull() shouldContain "Hello Card"
        cleanupHost(host)
    }

    test("renders header and footer when provided") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Card(
                header = { div { +"Header" } },
                footer = { div { +"Footer" } },
            ) {
                div { +"Body" }
            }
        }

        val root = host.querySelector("div").shouldNotBeNull()
        val text = root.textContent ?: ""

        text shouldContain "Header"
        text shouldContain "Body"
        text shouldContain "Footer"

        cleanupHost(host)
    }

    test("applies clickable classes when onClick is provided") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Card(onClick = {}) {
                div { +"Click me" }
            }
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "cursor-pointer"
        root.className shouldContain "hover:shadow-md"

        cleanupHost(host)
    }

    test("does not apply clickable classes when onClick is null") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Card {
                div { +"No click" }
            }
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "border"

        cleanupHost(host)
    }

    test("applies selected styles when selected=true") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Card(selected = true) {
                div { +"Selected" }
            }
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "ring-2"
        root.className shouldContain "ring-primary"

        cleanupHost(host)
    }

    test("custom className is appended") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            Card(className = "my-card") {
                div { +"Custom" }
            }
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "my-card"

        cleanupHost(host)
    }
})
