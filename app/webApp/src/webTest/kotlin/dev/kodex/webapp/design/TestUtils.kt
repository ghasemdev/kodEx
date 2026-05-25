package dev.kodex.webapp.design

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event

/**
 * Minimal DOM node abstraction used by component tests.
 * Hides the platform-specific element type (org.w3c.dom on JS, web.dom on WASM).
 */
internal interface DomNode {
    /** First descendant matching [selector], or null if not found. */
    fun querySelector(selector: String): DomNode?
    fun querySelectorAll(selector: String): NodeList?
    fun hasAttribute(selector: String): Boolean
    fun dispatchEvent(event: Event): Boolean
    val textContent: String?
    val className: String

    /** True only for <button disabled>. */
    val disabled: Boolean
    val innerHTML: String
    fun getAttribute(name: String): String?
}

/**
 * Renders [content] into an isolated host element and waits for Compose to commit.
 * Platform-specific: JS uses a real browser DOM, WASM skips.
 */
internal expect suspend fun renderComponent(
    content: @Composable IComponent.() -> Unit,
): DomNode

/** Removes the host element from the document. Must be called at end of each DOM test. */
internal expect fun cleanupHost(host: DomNode)

/** True when running on the Kotlin/JS target (real DOM available). */
internal expect fun isJsTarget(): Boolean
