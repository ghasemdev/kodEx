package dev.kodex.webapp.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kodex.webapp.gsap.gsap
import js.objects.unsafeJso
import kotlinx.browser.document
import kotlinx.browser.window

private const val ANIM_DURATION = 0.28
private const val ANIM_Y = 12.0

@Composable
@Suppress("LabeledExpression")
fun IComponent.PageTransition(id: String = "page-transition", content: @Composable IComponent.() -> Unit) {
    div(id = id) {
        content()
    }

    LaunchedEffect(Unit) {
        val prefersReducedMotion = runCatching {
            window.matchMedia("(prefers-reduced-motion: reduce)").matches
        }.getOrDefault(false)
        if (prefersReducedMotion) return@LaunchedEffect

        val element = document.getElementById(id) ?: return@LaunchedEffect
        gsap.from(
            element,
            unsafeJso {
                opacity = 0.0
                y = ANIM_Y
                duration = ANIM_DURATION
                ease = "power2.out"
            },
        )
    }
}
