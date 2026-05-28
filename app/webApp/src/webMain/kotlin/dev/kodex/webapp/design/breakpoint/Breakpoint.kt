package dev.kodex.webapp.design.breakpoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.kilua.utils.isDom
import kotlinx.browser.window
import org.w3c.dom.events.Event

private fun currentBreakpoint(): BreakpointTier = when {
    !isDom -> BreakpointTier.Desktop
    window.innerWidth >= 1920 -> BreakpointTier.Tv
    window.innerWidth >= 1024 -> BreakpointTier.Desktop
    window.innerWidth >= 640 -> BreakpointTier.Tablet
    else -> BreakpointTier.Mobile
}

@Composable
fun rememberBreakpoint(): State<BreakpointTier> {
    val state = remember { mutableStateOf(currentBreakpoint()) }

    DisposableEffect(Unit) {
        val handler: (Event) -> Unit = { state.value = currentBreakpoint() }
        window.addEventListener("resize", handler)
        onDispose {
            window.removeEventListener("resize", handler)
        }
    }

    return state
}

enum class BreakpointTier { Mobile, Tablet, Desktop, Tv }
