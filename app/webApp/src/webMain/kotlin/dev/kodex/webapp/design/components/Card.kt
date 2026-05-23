package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div

@Composable
fun IComponent.Card(
    header: (@Composable IComponent.() -> Unit)? = null,
    footer: (@Composable IComponent.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    className: String? = null,
    content: @Composable IComponent.() -> Unit,
) {
    val clickableClasses = if (onClick != null)
        "cursor-pointer hover:shadow-md active:scale-[0.99] transition-all duration-150"
    else ""
    val selectedClasses = if (selected)
        "ring-2 ring-primary border-primary"
    else "border-outline/20"

    div(
        className = "rounded-xl border bg-surface-container shadow-sm overflow-hidden " +
                "$clickableClasses $selectedClasses ${className ?: ""}".trim()
    ) {
        if (onClick != null) onClick { onClick() }

        if (header != null) {
            div(className = "px-4 py-3 border-b border-outline/10") { header() }
        }
        div(className = "p-4") { content() }
        if (footer != null) {
            div(className = "px-4 py-3 border-t border-outline/10 bg-surface/50") { footer() }
        }
    }
}
