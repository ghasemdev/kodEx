@file:Suppress("KotlinUnreachableCode", "LabeledExpression", "MagicNumber")

package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.span
import dev.kodex.webapp.gsap.gsap
import js.objects.unsafeJso
import kotlinx.browser.document

@Composable
fun IComponent.GsapSmokePreview() {
    val boxId = "gsap-smoke-box"
    val ready = remember { mutableStateOf(false) }

    div(className = "p-8 flex flex-col gap-4") {
        span(className = "text-sm text-on-surface/60") {
            +"Box should slide in from the left over 1 second on mount."
        }
        div(
            id = boxId,
            className = "w-32 h-32 bg-primary rounded-xl flex items-center justify-center " +
                "text-white font-bold text-lg opacity-0",
        ) {
            +"GSAP ✓"
        }
        if (ready.value) {
            span(className = "text-success text-sm font-medium") {
                +"✓ GSAP loaded and animated"
            }
        }
    }

    LaunchedEffect(Unit) {
        val element = document.getElementById(boxId) ?: return@LaunchedEffect

        gsap.fromTo(
            targets = element,
            fromVars = unsafeJso {
                x = -100.0
                opacity = 0.0
                ease = "power2.out"
            },
            toVars = unsafeJso {
                x = 0.0
                opacity = 1.0
                duration = 1.0
                ease = "power2.out"
                onComplete = {
                    ready.value = true
                }
            },
        )
    }
}
