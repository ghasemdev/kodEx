package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kilua.html.p
import dev.kodex.webapp.design.components.ThemeSwitcher

@Composable
fun IComponent.ThemeSwitcherPreview() {
    div(className = "flex flex-col gap-6") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Theme Toggle" }
        p(className = "text-sm text-on-surface/60") {
            +"Click the button to toggle between light and dark mode. The entire playground updates immediately."
        }
        div(className = "flex items-center gap-4") {
            ThemeSwitcher(
                id = "theme-switcher-main",
            )
            p(className = "text-sm text-on-surface/50") { +"← click to toggle" }
        }

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"In a Toolbar" }
        div(className = FLEX_ITEMS_CENTER_JUSTIFY_BETWEEN) {
            p(className = "text-sm font-medium text-on-surface") { +"KodEx Admin" }
            ThemeSwitcher(
                id = "theme-switcher-toolbar",
            )
        }
    }
}

private const val FLEX_ITEMS_CENTER_JUSTIFY_BETWEEN = "flex items-center justify-between px-4 py-2 rounded-xl border " +
    "border-outline/20 bg-surface-container"
