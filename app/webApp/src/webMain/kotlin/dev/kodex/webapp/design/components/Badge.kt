package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.span

@Composable
fun IComponent.Badge(
    variant: BadgeVariant = BadgeVariant.Default,
    className: String? = null,
    id: String? = null,
    content: @Composable IComponent.() -> Unit,
) {
    val colorClasses = when (variant) {
        BadgeVariant.Default -> "bg-surface-container text-on-surface"
        BadgeVariant.Primary -> "bg-primary/15 text-primary"
        BadgeVariant.Success -> "bg-success/15 text-success"
        BadgeVariant.Warning -> "bg-warning/15 text-warning"
        BadgeVariant.Danger -> "bg-error/15 text-error"
        BadgeVariant.Info -> "bg-info/15 text-info"
    }
    span(
        className = (
            "inline-flex items-center px-2.5 py-0.5 " +
                "rounded-full text-xs font-medium transition-colors duration-150 $colorClasses ${className ?: ""}"
            ).trim(),
        id = id,
    ) {
        content()
    }
}

enum class BadgeVariant { Default, Primary, Success, Warning, Danger, Info }
