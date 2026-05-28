package dev.kodex.webapp.design.breakpoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.kilua.utils.isDom
import kotlinx.browser.window
import org.w3c.dom.events.Event

private const val TV_WIDTH = 1920
private const val DESKTOP_WIDTH = 1024
private const val MOBILE_WIDTH = 640

private fun currentBreakpoint(): BreakpointTier = when {
    !isDom -> BreakpointTier.Desktop
    window.innerWidth >= TV_WIDTH -> BreakpointTier.Tv
    window.innerWidth >= DESKTOP_WIDTH -> BreakpointTier.Desktop
    window.innerWidth >= MOBILE_WIDTH -> BreakpointTier.Tablet
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
