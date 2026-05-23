package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.form.InputType
import dev.kilua.form.text.text
import dev.kilua.html.div
import dev.kilua.html.label
import dev.kilua.html.p
import dev.kilua.html.span

@Composable
fun IComponent.Input(
    value: String? = null,
    type: InputType = InputType.Text,
    onValueChange: (String) -> Unit = {},
    label: String? = null,
    helperText: String? = null,
    error: String? = null,
    placeholder: String? = null,
    disabled: Boolean = false,
    required: Boolean = false,
    id: String? = null,
    className: String? = null,
) {
    val hasError = !error.isNullOrBlank()
    val inputId = id ?: label?.lowercase()?.replace(" ", "-")

    div(className = "flex flex-col gap-1 ${className ?: ""}".trim()) {
        if (label != null) {
            label(className = "text-sm font-medium text-on-surface") {
                if (inputId != null) htmlFor(inputId)
                +label
                if (required) span(className = "text-error ms-1") { +"*" }
            }
        }
        text(
            value = value,
            type = type,
            placeholder = placeholder,
            disabled = if (disabled) true else null,
            required = if (required) true else null,
            className = "w-full px-3 py-2 rounded-lg border text-sm bg-surface text-on-surface " +
                    "placeholder:text-on-surface/40 transition-colors duration-150 " +
                    "focus:outline-none focus:ring-2 " +
                    if (hasError) "border-error focus:ring-error/40"
                    else "border-outline focus:ring-primary/40 hover:border-outline-variant",
            id = inputId,
        ) {
            onInput { onValueChange(this.value ?: "") }
        }
        when {
            hasError -> p(className = "text-xs text-error") { +error!! }
            !helperText.isNullOrBlank() -> p(className = "text-xs text-on-surface/60") { +helperText }
        }
    }
}
