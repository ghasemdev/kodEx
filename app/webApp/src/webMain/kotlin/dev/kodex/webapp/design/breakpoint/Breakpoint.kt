package dev.kodex.webapp.design.breakpoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.kilua.utils.isDom
import web.cssom.MediaQuery
import web.cssom.matchMedia
import web.events.Event
import web.events.EventType
import web.events.addEventListener
import web.events.removeEventListener

private fun currentBreakpoint(): BreakpointTier = when {
    !isDom -> BreakpointTier.Desktop
    matchMedia(MediaQuery("(min-width: 1920px)")).matches -> BreakpointTier.Tv
    matchMedia(MediaQuery("(min-width: 1024px)")).matches -> BreakpointTier.Desktop
    matchMedia(MediaQuery("(min-width: 640px)")).matches -> BreakpointTier.Tablet
    else -> BreakpointTier.Mobile
}

@Composable
fun rememberBreakpoint(): State<BreakpointTier> {
    val state = remember { mutableStateOf(currentBreakpoint()) }

    DisposableEffect(Unit) {
        val handler: (Event) -> Unit = { state.value = currentBreakpoint() }
        val mq640 = matchMedia(MediaQuery("(min-width: 640px)"))
        val mq1024 = matchMedia(MediaQuery("(min-width: 1024px)"))
        val mq1920 = matchMedia(MediaQuery("(min-width: 1920px)"))
        val type = EventType<Event>("change")
        mq640.addEventListener(type, handler)
        mq1024.addEventListener(type, handler)
        mq1920.addEventListener(type, handler)
        onDispose {
            mq640.removeEventListener(type, handler)
            mq1024.removeEventListener(type, handler)
            mq1920.removeEventListener(type, handler)
        }
    }

    return state
}

enum class BreakpointTier { Mobile, Tablet, Desktop, Tv }
