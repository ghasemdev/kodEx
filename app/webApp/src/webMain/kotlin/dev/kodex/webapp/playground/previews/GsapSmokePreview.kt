package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.span
import dev.kodex.webapp.gsap.Gsap
import kotlinx.browser.document
import kotlin.js.js

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
        val el = document.getElementById(boxId) ?: return@LaunchedEffect
        Gsap.from(
            el.asDynamic().unsafeCast<kotlin.js.JsAny>(),
            js("""{ x: -100, opacity: 0, duration: 1, ease: "power2.out" }"""),
        )
        ready.value = true
    }
}
