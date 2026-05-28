package dev.kodex.webapp.screenshot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldStartWith
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

class ButtonScreenshotTest : FunSpec({
    test("capture screenshot") {
        val div = document.createElement("div") as HTMLDivElement

        div.innerHTML = """
            <button style="
                background: purple;
                color: white;
                padding: 12px;
                border-radius: 8px;
            ">
                Hello
            </button>
        """.trimIndent()

        document.body?.appendChild(div)

        val screenshot = captureScreenshot(div)
        screenshot.shouldStartWith("data:image/png")
    }
})
