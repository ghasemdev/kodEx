package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CodeBlockDomTest : FunSpec({
    test("renders pre and code elements") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            CodeBlock(code = "println(\"Hello\")")
        }

        host.querySelector("pre").shouldNotBeNull()
        host.querySelector("code").shouldNotBeNull()

        cleanupHost(host)
    }

    test("renders provided code text") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            CodeBlock(code = "val x = 10")
        }

        val codeEl = host.querySelector("code").shouldNotBeNull()
        codeEl.textContent.shouldNotBeNull() shouldContain "val x = 10"

        cleanupHost(host)
    }

    test("applies language class to code element") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            CodeBlock(
                code = "print(1)",
                language = "python"
            )
        }

        val codeEl = host.querySelector("code").shouldNotBeNull()
        codeEl.className shouldContain "language-python"

        cleanupHost(host)
    }

    test("renders copy button when enabled") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            CodeBlock(code = "hello", showCopyButton = true)
        }

        val button = host.querySelector("span")
        button.shouldNotBeNull()

        cleanupHost(host)
    }

    test("does not render copy button when disabled") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            CodeBlock(code = "hello", showCopyButton = false)
        }

        val button = host.querySelector("span")
        button shouldBe null

        cleanupHost(host)
    }

    test("root has correct container classes") {
        if (!isJsTarget()) return@test

        val host = renderComponent {
            CodeBlock(code = "x")
        }

        val root = host.querySelector("div").shouldNotBeNull()
        root.className shouldContain "rounded-xl"
        root.className shouldContain "border"

        cleanupHost(host)
    }
})
