package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.button

@Composable
fun IComponent.Button(
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ComponentSize = ComponentSize.Md,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    label: String? = null,
    icon: String? = null,
    className: String? = null,
    id: String? = null,
    content: @Composable IComponent.() -> Unit = {},
) {
    val variantClasses = when (variant) {
        ButtonVariant.Primary ->
            "bg-primary text-white hover:bg-primary/90 focus:ring-2 focus:ring-primary/50"

        ButtonVariant.Secondary ->
            "bg-surface-container text-on-surface border border-outline " +
                "hover:bg-surface-variant focus:ring-2 focus:ring-outline"

        ButtonVariant.Ghost ->
            "bg-transparent text-primary hover:bg-primary/10 focus:ring-2 focus:ring-primary/30"

        ButtonVariant.Danger ->
            "bg-error text-white hover:bg-error/90 focus:ring-2 focus:ring-error/50"

        ButtonVariant.Link ->
            "bg-transparent text-primary underline hover:text-primary/80 focus:ring-0 p-0"
    }
    val sizeClasses = when (size) {
        ComponentSize.Sm -> "px-3 py-1.5 text-sm rounded-md"
        ComponentSize.Md -> "px-4 py-2 text-base rounded-lg"
        ComponentSize.Lg -> "px-6 py-3 text-lg rounded-xl"
    }
    val baseClasses = "inline-flex items-center gap-2 font-medium transition-colors duration-150 " +
        "focus:outline-none disabled:opacity-50 disabled:pointer-events-none"

    button(
        label = label,
        icon = icon,
        id = id,
        disabled = if (enabled) null else true,
        className = "$baseClasses $variantClasses $sizeClasses ${className ?: ""}".trim(),
    ) {
        onClick { onClick() }
        content()
    }
}

enum class ButtonVariant { Primary, Secondary, Ghost, Danger, Link }
enum class ComponentSize { Sm, Md, Lg }
