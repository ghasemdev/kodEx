package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kilua.html.p
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.components.Modal

@Composable
fun IComponent.ModalPreview() {
    var infoOpen by remember { mutableStateOf(false) }
    var confirmOpen by remember { mutableStateOf(false) }

    div(className = "flex flex-col gap-6") {
        h3(className = "text-lg font-semibold text-on-surface") { +"Info Modal" }
        Button(label = "Open info modal", onClick = { infoOpen = true })

        h3(className = "text-lg font-semibold text-on-surface") { +"Confirm Modal" }
        Button(variant = ButtonVariant.Danger, label = "Delete exam", onClick = { confirmOpen = true })
    }

    Modal(visible = infoOpen, onDismiss = { infoOpen = false }, title = "About KodEx") {
        div(className = "p-4 flex flex-col gap-4") {
            p(className = "text-sm text-on-surface/70") {
                +"KodEx is an interactive Kotlin exam platform. Candidates write real code evaluated by a test-injection engine."
            }
            Button(label = "Got it", onClick = { infoOpen = false })
        }
    }

    Modal(visible = confirmOpen, onDismiss = { confirmOpen = false }, title = "Confirm deletion") {
        div(className = "p-4 flex flex-col gap-4") {
            p(className = "text-sm text-on-surface/70") {
                +"This action is irreversible. The exam and all candidate results will be permanently deleted."
            }
            div(className = "flex gap-3 justify-end") {
                Button(variant = ButtonVariant.Secondary, label = "Cancel", onClick = { confirmOpen = false })
                Button(variant = ButtonVariant.Danger, label = "Delete", onClick = { confirmOpen = false })
            }
        }
    }
}
