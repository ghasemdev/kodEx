@file:Suppress("LabeledExpression")

package dev.kodex.webapp.landing

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.isJsTarget
import dev.kodex.webapp.design.renderComponent
import dev.kodex.webapp.layout.Footer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class FooterTest : FunSpec({
    context("footer root") {
        test("site-footer element is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.querySelector("#site-footer").shouldNotBeNull()
            cleanupHost(host)
        }

        test("footer contains 'KodEx' brand text") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.textContent shouldContain "KodEx"
            cleanupHost(host)
        }
    }

    context("footer columns") {
        test("Platform column heading is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.textContent shouldContain "Platform"
            cleanupHost(host)
        }

        test("Legal column heading is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.textContent shouldContain "Legal"
            cleanupHost(host)
        }

        test("Problems link is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.textContent shouldContain "Problems"
            cleanupHost(host)
        }

        test("Privacy Policy link is rendered") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.textContent shouldContain "Privacy Policy"
            cleanupHost(host)
        }
    }

    context("social icons") {
        test("renders exactly 4 social icon elements") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            val icons = host.querySelectorAll("[id^='footer-social-']")
            icons?.length shouldBe 4
            cleanupHost(host)
        }

        test("GitHub social icon is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.querySelector("#footer-social-gh").shouldNotBeNull()
            cleanupHost(host)
        }

        test("Twitter/X social icon is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.querySelector("#footer-social-tw").shouldNotBeNull()
            cleanupHost(host)
        }

        test("Telegram social icon is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.querySelector("#footer-social-tg").shouldNotBeNull()
            cleanupHost(host)
        }

        test("Discord social icon is present") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            host.querySelector("#footer-social-dc").shouldNotBeNull()
            cleanupHost(host)
        }

        test("GitHub icon has correct aria-label") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            val gh = host.querySelector("#footer-social-gh")
            gh.shouldNotBeNull()
            gh.getAttribute("aria-label") shouldContain "GitHub"
            cleanupHost(host)
        }

        test("social icons have role link") {
            if (!isJsTarget()) return@test
            val host = renderComponent { Footer() }
            val gh = host.querySelector("#footer-social-gh")
            gh.shouldNotBeNull()
            gh.getAttribute("role") shouldBe "link"
            cleanupHost(host)
        }
    }
})
