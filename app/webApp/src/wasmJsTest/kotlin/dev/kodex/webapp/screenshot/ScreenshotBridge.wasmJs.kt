package dev.kodex.webapp.screenshot

import org.w3c.dom.HTMLElement

actual suspend fun captureScreenshot(element: HTMLElement): String {
    error("This method should not be called")
}
