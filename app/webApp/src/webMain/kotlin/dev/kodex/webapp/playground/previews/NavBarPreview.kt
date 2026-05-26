package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kilua.html.p
import dev.kodex.webapp.design.components.LanguageSwitcher
import dev.kodex.webapp.design.components.NavBar
import dev.kodex.webapp.design.components.NavItem
import dev.kodex.webapp.design.components.ThemeSwitcher

private val NAV_ITEMS = listOf(
    NavItem("home", "Home", "fa-solid fa-house"),
    NavItem("exams", "Exams", "fa-solid fa-file-code"),
    NavItem("results", "Results", "fa-solid fa-chart-bar"),
    NavItem("profile", "Profile", "fa-solid fa-circle-user"),
)

@Composable
fun IComponent.NavBarPreview() {
    var active by remember { mutableStateOf("home") }

    div(className = "flex flex-col gap-6") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"NavBar (resize window to see mobile bottom bar)" }
        // overflow-visible so LanguageSwitcher dropdown is not clipped; NavBar itself carries rounded-xl
        div(className = "rounded-xl border border-outline/20") {
            NavBar(
                items = NAV_ITEMS,
                selectedItem = active,
                onItemSelect = { active = it },
                className = "rounded-xl",
                actions = {
                    ThemeSwitcher()
                    LanguageSwitcher()
                },
            )
        }
        p(className = "text-sm text-on-surface/50") {
            +"Selected: $active"
        }
    }
}
