package dev.kodex.webapp.turnstile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import js.objects.unsafeJso
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.coroutines.delay

private const val POLL_INTERVAL_MS = 100L
private const val MAX_POLL_ATTEMPTS = 50

@Suppress("LabeledExpression")
@Composable
fun IComponent.TurnstileWidget(id: String, siteKey: String, onVerified: (String) -> Unit) {
    var widgetId by remember { mutableStateOf<String?>(null) }

    div(id = id) {}

    LaunchedEffect(siteKey) {
        var attempts = 0
        while (turnstile == null && attempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS.milliseconds)
            attempts++
        }
        val api = turnstile ?: return@LaunchedEffect
        val element = document.getElementById(id) ?: return@LaunchedEffect
        widgetId = api.render(
            element,
            unsafeJso {
                sitekey = siteKey
                callback = onVerified
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            widgetId?.let { current -> runCatching { turnstile?.remove(current) } }
        }
    }
}
