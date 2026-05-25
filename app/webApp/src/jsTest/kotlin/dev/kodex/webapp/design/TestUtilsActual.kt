package dev.kodex.webapp.design

import androidx.compose.runtime.Composable
import dev.kilua.compose.root
import dev.kilua.core.IComponent
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event

private var hostCounter = 0

// --- DomNode adapter around org.w3c.dom.Element --------------------------------

private class JsDomNode(val el: Element) : DomNode {
    override fun querySelector(selector: String): DomNode? =
        el.querySelector(selector)?.let { JsDomNode(it) }

    override fun querySelectorAll(selector: String): NodeList =
        el.querySelectorAll(selector)

    override fun hasAttribute(selector: String): Boolean = el.hasAttribute(selector)
    override fun dispatchEvent(event: Event): Boolean = el.dispatchEvent(event)

    override val textContent: String?
        get() = el.textContent

    override val className: String
        get() = el.className

    override val disabled: Boolean
        get() = (el as? HTMLButtonElement)?.disabled ?: false

    override val innerHTML: String
        get() = el.innerHTML

    override fun getAttribute(name: String): String? =
        el.getAttribute(name)
}

// --- expect actuals -----------------------------------------------------------

internal actual suspend fun renderComponent(
    content: @Composable IComponent.() -> Unit,
): DomNode {
    val id = "test-host-${++hostCounter}"
    val host = document.createElement("div")
    host.setAttribute("id", id)
    document.body!!.appendChild(host)
    root(id) { content() }
    delay(80.milliseconds)
    return JsDomNode(host)
}

internal actual fun cleanupHost(host: DomNode) {
    runCatching {
        val el = (host as JsDomNode).el
        document.body?.removeChild(el as Node)
    }
}

internal actual fun isJsTarget(): Boolean = true
