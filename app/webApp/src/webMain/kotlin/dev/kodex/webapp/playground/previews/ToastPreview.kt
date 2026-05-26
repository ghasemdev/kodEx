package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.components.ToastContainer
import dev.kodex.webapp.design.components.ToastLevel
import dev.kodex.webapp.design.components.ToastStore

@Composable
fun IComponent.ToastPreview() {
    div(className = "flex flex-col gap-6") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Trigger Toasts" }
        div(className = "flex flex-wrap gap-3") {
            Button(
                label = "Info",
                variant = ButtonVariant.Secondary,
                onClick = { ToastStore.show("Exam submitted successfully.", ToastLevel.Info) },
            )
            Button(
                label = "Success",
                variant = ButtonVariant.Secondary,
                onClick = { ToastStore.show("Score saved: 98 / 100", ToastLevel.Success) },
            )
            Button(
                label = "Warning",
                variant = ButtonVariant.Secondary,
                onClick = { ToastStore.show("Time is running low — 2 minutes left.", ToastLevel.Warning) },
            )
            Button(
                label = "Error",
                variant = ButtonVariant.Danger,
                onClick = { ToastStore.show("Compilation failed: syntax error on line 12.", ToastLevel.Error) },
            )
        }
    }

    // Toast host — renders floating toasts in bottom-right corner
    ToastContainer()
}
