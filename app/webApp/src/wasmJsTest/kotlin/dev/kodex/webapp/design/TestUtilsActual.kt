package dev.kodex.webapp.design

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event

// WASM-JS does not expose the same DOM type system as Kotlin/JS.
// DOM component tests run only on the JS target; call sites are guarded by the io.kotest.assumptions DSL.

private object NoOpDomNode : DomNode {
    override val textContent: String? get() = null
    override val className: String get() = ""
    override val disabled: Boolean get() = false
    override val innerHTML: String = ""

    override fun querySelector(selector: String): DomNode? = null
    override fun querySelectorAll(selector: String): NodeList? = null
    override fun hasAttribute(selector: String) = false
    override fun dispatchEvent(event: Event): Boolean = false
    override fun getAttribute(name: String): String? = null
}

internal actual suspend fun renderComponent(content: @Composable IComponent.() -> Unit): DomNode = NoOpDomNode

internal actual fun cleanupHost(host: DomNode) {
    // no-op
}

internal actual fun isJsTarget(): Boolean = false
