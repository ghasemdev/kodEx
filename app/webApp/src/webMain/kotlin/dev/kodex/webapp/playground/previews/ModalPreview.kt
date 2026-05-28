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
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Info Modal" }
        Button(
            label = "Open info modal",
            id = "open-info-modal",
            onClick = { infoOpen = true },
        )

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Confirm Modal" }
        Button(
            variant = ButtonVariant.Danger,
            id = "open-confirm-modal",
            label = "Delete exam",
            onClick = { confirmOpen = true },
        )
    }

    Modal(
        visible = infoOpen,
        onDismiss = { infoOpen = false },
        id = "info-modal",
        title = "About KodEx",
    ) {
        div(className = "p-4 flex flex-col gap-4") {
            p(className = "text-sm text-on-surface/70") {
                +MODAL_TEXT
            }
            Button(label = "Got it", onClick = { infoOpen = false })
        }
    }

    Modal(
        visible = confirmOpen,
        onDismiss = { confirmOpen = false },
        id = "confirm-modal",
        title = "Confirm deletion",
    ) {
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

private const val MODAL_TEXT = "KodEx is an interactive Kotlin exam platform. " +
    "Candidates write real code evaluated by a test-injection engine."
