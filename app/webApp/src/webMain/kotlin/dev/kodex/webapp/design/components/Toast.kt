package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.span
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToastLevel { Info, Success, Warning, Error }

data class ToastMessage(
    val id: Long = Clock.System.now().toEpochMilliseconds(),
    val message: String,
    val level: ToastLevel = ToastLevel.Info,
    val duration: Duration = 3500.milliseconds,
)

object ToastStore {
    val TOASTS = mutableStateListOf<ToastMessage>()

    fun show(message: String, level: ToastLevel = ToastLevel.Info, duration: Duration = 3500.milliseconds) {
        val toast = ToastMessage(message = message, level = level, duration = duration)
        TOASTS.add(toast)
        MainScope().launch {
            delay(duration)
            TOASTS.remove(toast)
        }
    }

    fun dismiss(toast: ToastMessage) {
        TOASTS.remove(toast)
    }
}

@Composable
fun IComponent.ToastContainer() {
    if (ToastStore.TOASTS.isEmpty()) return

    div(
        id = "toast-container",
        className = "fixed bottom-4 end-4 z-50 flex flex-col gap-2 max-w-sm w-full",
    ) {
        ToastStore.TOASTS.forEach { toast ->
            val (bg, icon) = when (toast.level) {
                ToastLevel.Info -> "bg-surface-container border-outline/20 text-on-surface" to "ℹ"
                ToastLevel.Success -> "bg-success/15 border-success/30 text-success" to "✓"
                ToastLevel.Warning -> "bg-warning/15 border-warning/30 text-warning" to "⚠"
                ToastLevel.Error -> "bg-error/15 border-error/30 text-error" to "X"
            }
            div(
                id = "toast-${toast.id}",
                className = "flex items-center gap-3 px-4 py-3 rounded-xl border shadow-lg $bg toast-slide-in",
            ) {
                role(if (toast.level == ToastLevel.Error) "alert" else "status")
                span(className = "text-base flex-shrink-0 w-5 text-center leading-none") { +icon }
                span(className = "flex-1 text-sm") { +toast.message }
                span(
                    className = "cursor-pointer flex-shrink-0 w-6 h-6 flex items-center justify-center " +
                        "rounded-full opacity-50 hover:opacity-100 hover:bg-black/10 " +
                        "transition-all text-xs leading-none",
                ) {
                    +"✕"
                    onClick { ToastStore.dismiss(toast) }
                }
            }
        }
    }
}
