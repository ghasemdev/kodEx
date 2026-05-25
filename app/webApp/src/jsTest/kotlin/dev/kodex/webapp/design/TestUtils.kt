package dev.kodex.webapp.design

import androidx.compose.runtime.Composable
import dev.kilua.compose.root
import dev.kilua.core.IComponent
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.w3c.dom.Element
import org.w3c.dom.Node

private var hostCounter = 0

/**
 * Renders a Kilua composable into a temporary div and waits for Compose to commit.
 * Returns the host element for DOM assertions; call [cleanupHost] when done.
 */
internal suspend fun renderComponent(
    content: @Composable IComponent.() -> Unit,
): Element {
    val id = "test-host-${++hostCounter}"
    val host = document.createElement("div")
    host.setAttribute("id", id)
    document.body!!.appendChild(host)
    root(id) { content() }
    // Yield to the microtask queue long enough for Compose to run its initial composition.
    delay(80.milliseconds)
    return host
}

internal fun cleanupHost(host: Element) {
    runCatching { document.body?.removeChild(host as Node) }
}
