package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kodex.webapp.design.components.Badge
import dev.kodex.webapp.design.components.BadgeVariant

@Composable
fun IComponent.BadgePreview() {
    div(className = "flex flex-col gap-6") {
        h3(className = "text-lg font-semibold text-on-surface") { +"Badge Variants" }
        div(className = "flex flex-wrap gap-3 items-center") {
            BadgeVariant.entries.forEach { variant ->
                Badge(variant = variant) { +variant.name }
            }
        }

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"With Icons" }
        div(className = "flex flex-wrap gap-3 items-center") {
            Badge(variant = BadgeVariant.Success) { +"✓  Passed" }
            Badge(variant = BadgeVariant.Danger) { +"✗  Failed" }
            Badge(variant = BadgeVariant.Warning) { +"⚠  Pending" }
            Badge(variant = BadgeVariant.Info) { +"ℹ  Review" }
            Badge(variant = BadgeVariant.Primary) { +"★  Featured" }
        }

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"In Context" }
        div(className = "flex flex-wrap gap-3 items-center text-base text-on-surface") {
            +"Score"
            Badge(variant = BadgeVariant.Success) { +"98 / 100" }
            +"Status"
            Badge(variant = BadgeVariant.Primary) { +"Live" }
            +"Difficulty"
            Badge(variant = BadgeVariant.Warning) { +"Medium" }
        }
    }
}
