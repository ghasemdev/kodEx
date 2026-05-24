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
        h3(className = "text-lg font-semibold text-on-surface") { +"Language Switcher" }
        p(className = "text-sm text-on-surface/60") {
            +"Switches the app locale between English and Persian (فارسی). Persian activates RTL layout and loads the Vazirmatn variable font."
        }
        div(className = "flex items-center gap-4") {
            LanguageSwitcher()
        }

        h3(className = "text-lg font-semibold text-on-surface") { +"Together in a Toolbar" }
        div(className = "flex items-center justify-between px-4 py-2 rounded-xl border border-outline/20 bg-surface-container") {
            p(className = "text-sm font-medium text-on-surface") { +"KodEx Admin" }
            div(className = "flex items-center gap-2") {
                ThemeSwitcher()
                LanguageSwitcher()
            }
        }
    }
}
