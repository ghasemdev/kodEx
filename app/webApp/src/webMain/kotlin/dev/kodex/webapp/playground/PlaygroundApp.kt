package dev.kodex.webapp.playground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h1
import dev.kilua.html.h2
import dev.kilua.html.span
import dev.kodex.webapp.design.breakpoint.BreakpointTier
import dev.kodex.webapp.design.breakpoint.rememberBreakpoint
import dev.kodex.webapp.playground.previews.BadgePreview
import dev.kodex.webapp.playground.previews.ButtonPreview
import dev.kodex.webapp.playground.previews.CardPreview
import dev.kodex.webapp.playground.previews.CodeBlockPreview
import dev.kodex.webapp.playground.previews.InputPreview
import dev.kodex.webapp.playground.previews.LanguageSwitcherPreview
import dev.kodex.webapp.playground.previews.ModalPreview
import dev.kodex.webapp.playground.previews.NavBarPreview
import dev.kodex.webapp.playground.previews.SidebarPreview
import dev.kodex.webapp.playground.previews.ThemeSwitcherPreview
import dev.kodex.webapp.playground.previews.ToastPreview
import dev.kodex.webapp.playground.previews.TypographyPreview

private val playgroundEntries: List<PlaygroundEntry> by lazy {
    buildPlaygroundEntries(
        listOf(
            // Foundation
            PlaygroundEntry("Typography", "Foundation") { TypographyPreview() },

            // Atoms
            PlaygroundEntry("Button",    "Atoms") { ButtonPreview() },
            PlaygroundEntry("Badge",     "Atoms") { BadgePreview() },
            PlaygroundEntry("Input",     "Atoms") { InputPreview() },

            // Molecules
            PlaygroundEntry("Card",      "Molecules") { CardPreview() },
            PlaygroundEntry("CodeBlock", "Molecules") { CodeBlockPreview() },
            PlaygroundEntry("Modal",     "Molecules") { ModalPreview() },
            PlaygroundEntry("Toast",     "Molecules") { ToastPreview() },

            // Navigation
            PlaygroundEntry("NavBar",    "Navigation") { NavBarPreview() },
            PlaygroundEntry("Sidebar",   "Navigation") { SidebarPreview() },

            // Controls
            PlaygroundEntry("ThemeSwitcher",    "Controls") { ThemeSwitcherPreview() },
            PlaygroundEntry("LanguageSwitcher", "Controls") { LanguageSwitcherPreview() },
        )
    )
}

@Composable
fun IComponent.PlaygroundApp() {
    val entries = playgroundEntries
    var selected by remember { mutableStateOf(entries.firstOrNull()?.name) }
    val breakpoint by rememberBreakpoint()
    val isMobile = breakpoint == BreakpointTier.Mobile

    div(className = "flex h-screen bg-surface overflow-hidden") {
        // Sidebar nav
        if (!isMobile) {
            div(className = "w-52 flex-shrink-0 border-e border-outline/20 bg-surface-container overflow-y-auto") {
                div(className = "px-4 py-3 border-b border-outline/10") {
                    h1(className = "text-sm font-semibold text-primary") { +"Playground" }
                }
                entries.groupBy { it.group }.forEach { (group, items) ->
                    h2(className = "px-4 pt-4 pb-1 text-xs font-semibold text-on-surface/50 uppercase tracking-wider") {
                        +group
                    }
                    items.forEach { entry ->
                        div(
                            className = "flex items-center px-4 py-2 text-sm cursor-pointer rounded-lg mx-2 " +
                                    "transition-colors duration-100 " +
                                    if (entry.name == selected) "bg-primary/10 text-primary font-medium"
                                    else "text-on-surface/70 hover:bg-surface-variant"
                        ) {
                            +entry.name
                            onClick { selected = entry.name }
                        }
                    }
                }
            }
        }

        // Canvas
        div(className = "flex-1 overflow-auto p-6") {
            val current = entries.find { it.name == selected }
            if (current != null) {
                div(className = "mb-4") {
                    h1(className = "text-2xl font-bold text-on-surface") { +current.name }
                    span(className = "text-sm text-on-surface/50") { +current.group }
                }
                div(className = "rounded-xl border border-outline/20 p-6 bg-surface") {
                    current.content(this)
                }
            } else {
                div(className = "flex items-center justify-center h-64 text-on-surface/40") {
                    +"Select a component from the sidebar"
                }
            }
        }
    }
}
