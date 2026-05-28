package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kodex.webapp.design.components.Input
import dev.kodex.webapp.design.components.TextArea

@Composable
fun IComponent.InputPreview() {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    div(className = "flex flex-col gap-6 max-w-md") {
        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Text Input" }
        Input(
            value = email,
            label = "Email",
            id = "input-email",
            placeholder = "you@example.com",
            helperText = "We'll never share your email.",
            onValueChange = { email = it },
        )

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"With Error" }
        Input(
            value = pass,
            label = "Password",
            id = "input-password",
            placeholder = "••••••••",
            error = if (pass.length in 1..7) "At least 8 characters required" else null,
            onValueChange = { pass = it },
        )

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"Disabled" }
        Input(
            value = "read-only value",
            label = "Username",
            id = "input-disabled",
            disabled = true,
            onValueChange = {},
        )

        h3(className = TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE) { +"TextArea" }
        TextArea(
            value = notes,
            label = "Notes",
            id = "textarea-notes",
            placeholder = "Write something…",
            helperText = "Markdown supported.",
            maxLength = 200,
            showCounter = true,
            rows = 4,
            onValueChange = { notes = it },
        )
    }
}

internal const val TEXT_LG_FONT_SEMIBOLD_TEXT_ON_SURFACE = "text-lg font-semibold text-on-surface"
