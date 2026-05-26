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
import dev.kodex.webapp.design.components.Badge
import dev.kodex.webapp.design.components.BadgeVariant
import dev.kodex.webapp.design.components.Card

@Composable
fun IComponent.CardPreview() {
    var selected by remember { mutableStateOf<Int?>(null) }

    div(className = "flex flex-col gap-6") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Basic Card" }
        Card {
            div(className = "p-4") {
                p(className = "text-base text-on-surface") { +"A simple surface-container card." }
                p(className = "text-sm text-on-surface/60 mt-1") { +"No header or footer." }
            }
        }

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"With Header & Footer" }
        Card(
            header = {
                div(className = "flex items-center justify-between") {
                    span(className = "font-semibold text-on-surface") { +"Kotlin Basics" }
                    Badge(variant = BadgeVariant.Primary) { +"Easy" }
                }
            },
            footer = {
                div(className = "flex items-center justify-between text-xs text-on-surface/50") {
                    span { +"10 questions" }
                    span { +"45 minutes" }
                }
            },
        ) {
            div(className = "p-4") {
                p(className = "text-sm text-on-surface/70") {
                    +"Covers val/var, data classes, null safety, and basic collections."
                }
            }
        }

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Selectable Cards" }
        div(className = "grid grid-cols-1 sm:grid-cols-2 gap-3") {
            listOf(0, 1, 2, 3).forEach { i ->
                Card(
                    selected = selected == i,
                    onClick = { selected = if (selected == i) null else i },
                ) {
                    div(className = "p-4 flex items-center gap-3") {
                        div(
                            className = "w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center " +
                                "text-primary font-bold",
                        ) {
                            +"${i + 1}"
                        }
                        div {
                            p(className = "text-sm font-medium text-on-surface") { +"Option ${i + 1}" }
                            p(className = "text-xs text-on-surface/50") { +"Click to select" }
                        }
                    }
                }
            }
        }
    }
}
