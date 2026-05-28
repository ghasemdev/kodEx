package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h2
import dev.kilua.html.span
import kotlinx.browser.document
import org.w3c.dom.events.Event
import web.keyboard.KeyboardEvent

@Suppress("CAST_NEVER_SUCCEEDS")
@Composable
fun IComponent.Modal(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    className: String? = null,
    id: String? = null,
    content: @Composable IComponent.() -> Unit,
) {
    if (!visible) return

    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent

            if (keyboardEvent?.key == "Escape") {
                onDismiss()
            }
        }

        document.addEventListener(
            type = "keydown",
            callback = listener,
        )

        onDispose {
            document.removeEventListener(
                type = "keydown",
                callback = listener,
            )
        }
    }

    div(className = "fixed inset-0 z-50 flex items-center justify-center") {
        // Backdrop
        div(className = "absolute inset-0 bg-black/50 backdrop-blur-sm modal-backdrop-in") {
            onClick { onDismiss() }
        }

        // Dialog
        div(
            className = "relative z-10 w-full max-w-md mx-4 rounded-2xl bg-surface shadow-xl " +
                "border border-outline/20 modal-dialog-in ${className ?: ""}".trim(),
            id = id,
        ) {
            role("dialog")
            attribute("aria-modal", "true")

            if (title != null) {
                attribute("aria-labelledby", "modal-title")
            }

            if (title != null) {
                div(className = "flex items-center justify-between px-5 py-4 border-b border-outline/10") {
                    h2(
                        className = "text-lg font-semibold text-on-surface",
                        id = "modal-title",
                    ) {
                        +title
                    }

                    span(
                        className = "cursor-pointer w-8 h-8 flex items-center justify-center rounded-full " +
                            "hover:bg-surface-variant text-on-surface/60 hover:text-on-surface transition-colors",
                    ) {
                        +"✕"

                        onClick {
                            onDismiss()
                        }
                    }
                }
            }

            div(className = "p-5") {
                content()
            }
        }
    }
}
