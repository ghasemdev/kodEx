package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.theme.themeSwitcher

@Composable
fun IComponent.ThemeSwitcher(className: String? = null) {
    themeSwitcher(
        round = true,
        lightIcon = "fa-solid fa-moon",
        darkIcon = "fa-solid fa-sun",
        autoIcon = "fa-solid fa-circle-half-stroke",
        className = "w-9 h-9 rounded-full flex items-center justify-center " +
            "bg-surface-container text-on-surface " +
            "hover:bg-primary hover:text-on-primary " +
            "transition-colors duration-200 cursor-pointer " +
            (className ?: ""),
    )
}
