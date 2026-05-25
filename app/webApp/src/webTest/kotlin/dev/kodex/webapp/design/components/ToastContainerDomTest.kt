package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import org.w3c.dom.asList

class ToastContainerDomTest : FunSpec({
    test("does not render when no toasts exist") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()

        val host = renderComponent {
            ToastContainer()
        }

        host.innerHTML.trim().shouldBeEmpty()

        cleanupHost(host)
    }

    test("renders toast container when toast exists") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show("Hello Toast")

        val host = renderComponent {
            ToastContainer()
        }

        val toast = host.querySelector("div")
        toast.shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders toast message text") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show("Saved successfully")

        val host = renderComponent {
            ToastContainer()
        }

        host.textContent.shouldNotBeNull() shouldContain "Saved successfully"

        cleanupHost(host)
    }

    test("renders correct role for error toast") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show(
            message = "Something failed",
            level = ToastLevel.Error
        )

        val host = renderComponent {
            ToastContainer()
        }

        val alert = host.querySelector("[role='alert']")
        alert.shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders success icon") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show(
            message = "Done",
            level = ToastLevel.Success
        )

        val host = renderComponent {
            ToastContainer()
        }

        host.textContent shouldContain "✓"

        cleanupHost(host)
    }

    test("renders error icon") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show(
            message = "Failed",
            level = ToastLevel.Error
        )

        val host = renderComponent {
            ToastContainer()
        }

        host.textContent shouldContain "✕"

        cleanupHost(host)
    }

    test("toast has dismiss button") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show("Dismiss me")

        val host = renderComponent {
            ToastContainer()
        }

        val closeBtn = host.querySelectorAll("span")
            ?.asList()
            ?.any { it.textContent == "✕" }

        closeBtn.shouldBeTrue()

        cleanupHost(host)
    }

    test("applies correct container classes") {
        if (!isJsTarget()) return@test

        ToastStore.toasts.clear()
        ToastStore.show("Test")

        val host = renderComponent {
            ToastContainer()
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "fixed"
        root.className shouldContain "bottom-4"
        root.className shouldContain "z-50"

        cleanupHost(host)
    }
})
