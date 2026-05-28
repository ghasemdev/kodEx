package dev.kodex.webapp.screenshot

import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement

actual suspend fun captureScreenshot(element: HTMLElement): String {
    val canvas = html2canvas(element).await()
    return canvas.toDataURL("image/png")
}
