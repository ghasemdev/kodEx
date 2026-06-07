package dev.kodex.webapp.pages

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h1
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.i18n.i18n
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
fun IComponent.NotFoundPage() {
    div(
        className = "min-h-screen flex flex-col items-center justify-center gap-6 " +
            "bg-surface text-on-surface px-4 text-center",
    ) {
        span(className = "text-8xl font-black text-primary/20 select-none") { +"404" }
        h1(className = "text-2xl font-bold text-on-surface") {
            +i18n.tr("Page not found")
        }
        p(className = "text-on-surface/60 max-w-sm") {
            +i18n.tr("The page you're looking for doesn't exist or has been moved.")
        }
        Button(
            label = i18n.tr("Back to Home"),
            variant = ButtonVariant.Primary,
            onClick = {
                window.history.pushState(null, "", "/")
                window.dispatchEvent(Event("popstate"))
            },
        )
    }
}
