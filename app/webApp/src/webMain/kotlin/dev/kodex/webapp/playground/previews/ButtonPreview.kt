package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.components.ComponentSize

@Composable
fun IComponent.ButtonPreview() {
    div(className = "flex flex-col gap-6") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Button Variants" }
        div(className = "flex flex-wrap gap-3") {
            ButtonVariant.entries.forEach { variant ->
                Button(variant = variant, label = variant.name)
            }
        }
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Button Sizes" }
        div(className = "flex flex-wrap items-center gap-3") {
            ComponentSize.entries.forEach { size ->
                Button(size = size, label = size.name)
            }
        }
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Disabled" }
        div(className = "flex flex-wrap gap-3") {
            Button(label = "Disabled", enabled = false)
        }
    }
}
