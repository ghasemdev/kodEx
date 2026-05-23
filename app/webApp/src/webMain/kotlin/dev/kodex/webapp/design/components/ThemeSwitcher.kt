package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.theme.themeSwitcher

@Composable
fun IComponent.ThemeSwitcher(className: String? = null) {
    themeSwitcher(
        round = true,
        className = "w-9 h-9 flex items-center justify-center " +
                "bg-surface-container text-on-surface " +
                "hover:bg-primary hover:text-white " +
                "transition-colors duration-200 " +
                (className ?: ""),
    )
}
