package dev.kodex.webapp.screenshot

import org.w3c.dom.HTMLElement

expect suspend fun captureScreenshot(element: HTMLElement): String
