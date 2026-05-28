package dev.kodex.webapp.design

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event

/**
 * Renders [content] into an isolated host element and waits for Compose to commit.
 * Platform-specific: JS uses a real browser DOM, WASM skips.
 */
internal expect suspend fun renderComponent(content: @Composable IComponent.() -> Unit): DomNode

/**
 * Minimal DOM node abstraction used by component tests.
 * Hides the platform-specific element type (org.w3c.dom on JS, web.dom on WASM).
 */
internal interface DomNode {
    val textContent: String?
    val className: String

    /** True only for <button disabled>. */
    val disabled: Boolean
    val innerHTML: String

    /** First descendant matching [selector], or null if not found. */
    fun querySelector(selector: String): DomNode?
    fun querySelectorAll(selector: String): NodeList?
    fun hasAttribute(selector: String): Boolean
    fun dispatchEvent(event: Event): Boolean
    fun getAttribute(name: String): String?
}

/** Removes the host element from the document. Must be called at end of each DOM test. */
internal expect fun cleanupHost(host: DomNode)

/** True when running on the Kotlin/JS target (real DOM available). */
internal expect fun isJsTarget(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
internal fun mouseEvent(): Event = js("new MouseEvent('click')")
