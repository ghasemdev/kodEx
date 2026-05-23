package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.aside
import dev.kilua.html.div
import dev.kilua.html.span
import dev.kodex.webapp.design.breakpoint.BreakpointTier
import dev.kodex.webapp.design.breakpoint.rememberBreakpoint

@Composable
fun IComponent.Sidebar(
    items: List<NavItem>,
    selectedItem: String? = null,
    onItemSelect: (String) -> Unit = {},
    header: (@Composable IComponent.() -> Unit)? = null,
    footer: (@Composable IComponent.() -> Unit)? = null,
    className: String? = null,
) {
    val breakpoint by rememberBreakpoint()

    // Hidden on Mobile
    if (breakpoint == BreakpointTier.Mobile) return

    // Icon-only rail on Tablet; full panel on Desktop/TV
    val isRail = breakpoint == BreakpointTier.Tablet
    var collapsed by remember { mutableStateOf(isRail) }
    val showCollapseToggle = breakpoint == BreakpointTier.Tablet

    aside(
        className = "flex flex-col h-full bg-surface-container border-e border-outline/20 " +
            "transition-all duration-200 " +
            (if (collapsed) "w-14" else "w-64") +
            " ${className ?: ""}".trim()
    ) {
        if (showCollapseToggle) {
            div(className = "flex justify-end p-2 border-b border-outline/10") {
                span(
                    className = "cursor-pointer w-8 h-8 flex items-center justify-center rounded-lg " +
                        "hover:bg-surface-variant text-on-surface/60 transition-colors"
                ) {
                    +(if (collapsed) "›" else "‹")
                    onClick { collapsed = !collapsed }
                }
            }
        }

        if (header != null && !collapsed) {
            div(className = "px-3 py-3 border-b border-outline/10") { header() }
        }

        div(className = "flex-1 overflow-y-auto py-2") {
            items.forEach { item ->
                val isSelected = item.key == selectedItem
                div(
                    className = "flex items-center gap-3 mx-2 px-3 py-2 rounded-lg cursor-pointer " +
                        "transition-colors duration-150 text-sm " +
                        if (isSelected) "bg-primary/10 text-primary font-medium"
                        else "text-on-surface/70 hover:bg-surface-variant hover:text-on-surface"
                ) {
                    if (item.icon != null) span(className = "${item.icon} w-5 text-center flex-shrink-0") {}
                    if (!collapsed) span(className = "truncate") { +item.label }
                    onClick { onItemSelect(item.key) }
                }
            }
        }

        if (footer != null && !collapsed) {
            div(className = "px-3 py-3 border-t border-outline/10") { footer() }
        }
    }
}
