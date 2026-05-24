package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.nav
import dev.kilua.html.span
import dev.kodex.webapp.design.breakpoint.BreakpointTier
import dev.kodex.webapp.design.breakpoint.rememberBreakpoint

data class NavItem(
    val key: String,
    val label: String,
    val icon: String? = null,
)

@Composable
fun IComponent.NavBar(
    items: List<NavItem>,
    selectedItem: String? = null,
    onItemSelect: (String) -> Unit = {},
    actions: (@Composable IComponent.() -> Unit)? = null,
    className: String? = null,
) {
    val breakpoint by rememberBreakpoint()
    val isMobile = breakpoint == BreakpointTier.Mobile

    if (!isMobile) {
        // Top bar for Tablet / Desktop / TV
        nav(
            className = "flex items-center justify-between h-14 px-4 " +
                "bg-surface-container border-b border-outline/20 ${className ?: ""}".trim()
        ) {
            div(className = "flex items-center gap-1") {
                items.forEach { item ->
                    val isSelected = item.key == selectedItem
                    div(
                        className = "flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm cursor-pointer " +
                            "transition-all duration-200 " +
                            if (isSelected) "bg-primary/10 text-primary font-medium"
                            else "text-on-surface/70 hover:bg-surface-variant hover:text-on-surface"
                    ) {
                        if (item.icon != null) span(className = "${item.icon} w-4 text-center") {}
                        span { +item.label }
                        onClick { onItemSelect(item.key) }
                    }
                }
            }
            if (actions != null) {
                div(className = "flex items-center gap-2") { actions() }
            }
        }
    } else {
        // Bottom bar on Mobile
        nav(
            className = "fixed bottom-0 start-0 end-0 z-40 " +
                "bg-surface-container border-t border-outline/20"
        ) {
            div(className = "flex items-stretch w-full") {
                items.forEach { item ->
                    val isSelected = item.key == selectedItem
                    div(
                        className = "flex-1 flex flex-col items-center justify-center gap-0.5 py-2 " +
                            "cursor-pointer transition-colors duration-150 text-xs " +
                            if (isSelected) "text-primary"
                            else "text-on-surface/60 hover:text-on-surface"
                    ) {
                        if (item.icon != null) span(className = "${item.icon} text-xl w-6 text-center") {}
                        span { +item.label }
                        onClick { onItemSelect(item.key) }
                    }
                }
            }
        }
    }
}
