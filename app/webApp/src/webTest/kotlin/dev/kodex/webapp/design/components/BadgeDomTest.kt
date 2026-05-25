package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

class BadgeDomTest : FunSpec({
    test("renders a <span> element") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Badge { +"Beta" } }
        host.querySelector("span").shouldNotBeNull()
        cleanupHost(host)
    }

    test("text content matches the slot") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Badge { +"New" } }
        val span = host.querySelector("span").shouldNotBeNull()
        span.textContent.shouldNotBeNull() shouldContain "New"
        cleanupHost(host)
    }

    test("has rounded-full class") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Badge { +"X" } }
        val span = host.querySelector("span").shouldNotBeNull()
        span.className shouldContain "rounded-full"
        cleanupHost(host)
    }

    test("custom className is appended") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Badge(className = "extra-class") { +"X" } }
        val span = host.querySelector("span").shouldNotBeNull()
        span.className shouldContain "extra-class"
        cleanupHost(host)
    }

    test("Primary variant has text-primary class") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Badge(variant = BadgeVariant.Primary) { +"P" } }
        val span = host.querySelector("span").shouldNotBeNull()
        span.className shouldContain "text-primary"
        cleanupHost(host)
    }

    test("Success variant has text-success class") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Badge(variant = BadgeVariant.Success) { +"OK" } }
        val span = host.querySelector("span").shouldNotBeNull()
        span.className shouldContain "text-success"
        cleanupHost(host)
    }
})
