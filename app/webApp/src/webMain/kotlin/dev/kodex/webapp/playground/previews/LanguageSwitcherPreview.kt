package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kilua.html.p
import dev.kodex.webapp.design.components.LanguageSwitcher
import dev.kodex.webapp.design.components.ThemeSwitcher

@Composable
fun IComponent.LanguageSwitcherPreview() {
    div(className = "flex flex-col gap-6") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Language Switcher" }
        p(className = "text-sm text-on-surface/60") {
            +LANGUAGE_SWITCHER_TEXT
        }
        div(className = "flex items-center gap-4") {
            LanguageSwitcher()
        }

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Together in a Toolbar" }
        div(className = FLEX_ITEMS_CENTER_JUSTIFY_BETWEEN) {
            p(className = "text-sm font-medium text-on-surface") { +"KodEx Admin" }
            div(className = "flex items-center gap-2") {
                ThemeSwitcher()
                LanguageSwitcher()
            }
        }
    }
}

private const val LANGUAGE_SWITCHER_TEXT = "Switches the app locale between English and Persian (فارسی). " +
    "Persian activates RTL layout and loads the Vazirmatn variable font."
private const val FLEX_ITEMS_CENTER_JUSTIFY_BETWEEN = "flex items-center justify-between px-4 py-2 rounded-xl " +
    "border border-outline/20 bg-surface-container"
