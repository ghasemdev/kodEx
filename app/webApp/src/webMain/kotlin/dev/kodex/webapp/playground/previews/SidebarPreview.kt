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
import dev.kilua.html.span
import dev.kodex.webapp.design.components.NavItem
import dev.kodex.webapp.design.components.Sidebar

private val sidebarItems = listOf(
    NavItem("dashboard",  "Dashboard",  "fa-solid fa-gauge"),
    NavItem("exams",      "Exams",      "fa-solid fa-file-code"),
    NavItem("candidates", "Candidates", "fa-solid fa-users"),
    NavItem("results",    "Results",    "fa-solid fa-chart-line"),
    NavItem("settings",   "Settings",   "fa-solid fa-gear"),
)

@Composable
fun IComponent.SidebarPreview() {
    var active by remember { mutableStateOf("dashboard") }

    div(className = "flex flex-col gap-6") {
        h3(className = "text-lg font-semibold text-on-surface") { +"Sidebar (hidden on mobile)" }
        div(className = "h-80 rounded-xl overflow-hidden border border-outline/20 flex") {
            Sidebar(
                items = sidebarItems,
                selectedItem = active,
                onItemSelect = { active = it },
                header = {
                    span(className = "text-sm font-bold text-primary") { +"KodEx Admin" }
                },
            )
            div(className = "flex-1 flex items-center justify-center bg-surface text-on-surface/40 text-sm") {
                +"Content area  —  selected: $active"
            }
        }
        p(className = "text-sm text-on-surface/50") {
            +"On Tablet the sidebar collapses to an icon rail."
        }
    }
}
