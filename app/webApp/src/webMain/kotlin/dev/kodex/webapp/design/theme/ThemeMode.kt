package dev.kodex.webapp.design.theme

import dev.kilua.theme.Theme

enum class ThemeMode {
    Light,
    Dark,
    Auto,
    ;

    fun toKiluaTheme(): Theme = when (this) {
        Light -> Theme.Light
        Dark -> Theme.Dark
        Auto -> Theme.Auto
    }

    companion object {
        fun fromKiluaTheme(theme: Theme): ThemeMode = when (theme) {
            Theme.Light -> Light
            Theme.Dark -> Dark
            Theme.Auto -> Auto
        }
    }
}

data class ThemeState(
    val mode: ThemeMode = ThemeMode.Auto,
    val isDark: Boolean = false,
)
