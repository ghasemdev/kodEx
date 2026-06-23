@file:Suppress("PropertyName", "VariableMinLength")

package dev.kodex.webapp.turnstile

import kotlin.js.JsAny
import org.w3c.dom.Element

// Loaded as a global by the Cloudflare script tag in index.html — not an npm/@JsModule import.
external val turnstile: TurnstileApi?

external interface TurnstileApi : JsAny {
    fun render(container: Element, options: TurnstileOptions): String
    fun reset(widgetId: String)
    fun remove(widgetId: String)
}

external interface TurnstileOptions : JsAny {
    var sitekey: String
    var callback: (String) -> Unit
    var theme: String
}
