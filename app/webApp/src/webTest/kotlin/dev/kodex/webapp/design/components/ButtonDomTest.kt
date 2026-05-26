@file:Suppress("LabeledExpression")

package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

class ButtonDomTest : FunSpec({
    test("renders a <button> element") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Button(label = "Submit") }
        host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        cleanupHost(host)
    }

    test("label appears in text content") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Button(label = "Click me") }
        val btn = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        btn.textContent.shouldNotBeNull() shouldContain "Click me"
        cleanupHost(host)
    }

    test("disabled=true sets the disabled attribute") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Button(label = "Save", enabled = false) }
        val btn = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        btn.disabled.shouldBeTrue()
        cleanupHost(host)
    }

    test("disabled=false does not set the disabled attribute") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Button(label = "Save", enabled = true) }
        val btn = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        btn.disabled.shouldBeFalse()
        cleanupHost(host)
    }

    test("custom className is present on the button element") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Button(label = "X", className = "my-custom") }
        val btn = host.querySelector(QUERY_SELECTOR).shouldNotBeNull()
        btn.className shouldContain "my-custom"
        cleanupHost(host)
    }
})

private const val QUERY_SELECTOR = "button"
