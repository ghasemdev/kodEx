package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ModalDomTest : FunSpec({
    test("visible=false renders nothing") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = false, onDismiss = {}) { +"Content" } }
        host.querySelector("[role='dialog']").shouldBeNull()
        cleanupHost(host)
    }

    test("visible=true renders an element with role=dialog") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = true, onDismiss = {}) { +"Content" } }
        host.querySelector("[role='dialog']").shouldNotBeNull()
        cleanupHost(host)
    }

    test("dialog has aria-modal=true") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = true, onDismiss = {}) { +"Hi" } }
        val dialog = host.querySelector("[role='dialog']").shouldNotBeNull()
        dialog.getAttribute("aria-modal") shouldBe "true"
        cleanupHost(host)
    }

    test("titled modal renders h2#modal-title") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = true, title = "Confirm Delete", onDismiss = {}) { +"Body" } }
        val h2 = host.querySelector("h2").shouldNotBeNull()
        h2.textContent.shouldNotBeNull() shouldContain "Confirm Delete"
        cleanupHost(host)
    }

    test("titled modal has aria-labelledby=modal-title") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = true, title = "Info", onDismiss = {}) { +"Body" } }
        val dialog = host.querySelector("[role='dialog']").shouldNotBeNull()
        dialog.getAttribute("aria-labelledby") shouldBe "modal-title"
        cleanupHost(host)
    }

    test("untitled modal has no h2") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = true, onDismiss = {}) { +"No title" } }
        host.querySelector("h2").shouldBeNull()
        cleanupHost(host)
    }

    test("body content is rendered inside the modal") {
        if (!isJsTarget()) return@test
        val host = renderComponent { Modal(visible = true, onDismiss = {}) { +"Slot body text" } }
        host.textContent.shouldNotBeNull() shouldContain "Slot body text"
        cleanupHost(host)
    }
})
