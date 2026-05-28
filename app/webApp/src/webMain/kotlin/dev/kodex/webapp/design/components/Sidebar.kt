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
@Suppress("CognitiveComplexMethod")
fun IComponent.Sidebar(
    items: List<NavItem>,
    selectedItem: String? = null,
    onItemSelect: (String) -> Unit = {},
    header: (@Composable IComponent.() -> Unit)? = null,
    footer: (@Composable IComponent.() -> Unit)? = null,
    className: String? = null,
    breakpointProvider: @Composable () -> BreakpointTier = { rememberBreakpoint().value },
) {
    val breakpoint = breakpointProvider()

    // Hidden on Mobile
    if (breakpoint == BreakpointTier.Mobile) return

    // Icon-only rail on Tablet; full panel on Desktop/TV
    val isRail = breakpoint == BreakpointTier.Tablet
    // remember(breakpoint) re-initializes when breakpoint changes so collapsed resets correctly
    var collapsed by remember(breakpoint) { mutableStateOf(isRail) }
    val showCollapseToggle = breakpoint == BreakpointTier.Tablet

    aside(
        className = "flex flex-col h-full bg-surface-container border-e border-outline/20 " +
            "transition-all duration-300 ease-in-out " +
            (if (collapsed) "w-14" else "w-64") +
            " ${className ?: ""}".trim(),
    ) {
        if (showCollapseToggle) {
            div(className = "flex justify-end p-2 border-b border-outline/10") {
                span(
                    className = "cursor-pointer w-8 h-8 flex items-center justify-center rounded-lg " +
                        "hover:bg-surface-variant text-on-surface/60 transition-colors",
                ) {
                    +(if (collapsed) "›" else "‹")
                    onClick { collapsed = !collapsed }
                }
            }
        }

        if (header != null && !collapsed) {
            div(className = "px-3 py-3 border-b border-outline/10") { header() }
        }

        div(className = "flex-1 overflow-y-auto") {
            items.forEach { item ->
                val isSelected = item.key == selectedItem
                div(
                    className = "flex items-center gap-3 px-4 py-2.5 cursor-pointer " +
                        "transition-all duration-200 active:scale-[0.98] text-sm " +
                        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 " +
                        if (isSelected) {
                            "bg-primary/10 text-primary font-medium border-e-2 border-primary"
                        } else {
                            "text-on-surface/70 hover:bg-surface-variant hover:text-on-surface"
                        },
                ) {
                    tabindex(0)
                    role("button")
                    if (isSelected) attribute("aria-current", "page")
                    if (item.icon != null) span(className = "${item.icon} w-4 text-center flex-shrink-0") {}
                    if (!collapsed) span(className = "truncate") { +item.label }
                    onClick { onItemSelect(item.key) }
                    onKeydown { e -> if (e.key == "Enter" || e.key == " ") onItemSelect(item.key) }
                }
            }
        }

        if (footer != null && !collapsed) {
            div(className = "px-3 py-3 border-t border-outline/10") { footer() }
        }
    }
}
