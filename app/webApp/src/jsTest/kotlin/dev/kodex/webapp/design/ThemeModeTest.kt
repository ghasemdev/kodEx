package dev.kodex.webapp.design

import dev.kilua.theme.Theme
import dev.kodex.webapp.design.theme.ThemeMode
import dev.kodex.webapp.design.theme.ThemeState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ThemeModeTest {

    @Test
    fun toKiluaTheme_mapsAllValues() {
        assertEquals(Theme.Light, ThemeMode.Light.toKiluaTheme())
        assertEquals(Theme.Dark, ThemeMode.Dark.toKiluaTheme())
        assertEquals(Theme.Auto, ThemeMode.Auto.toKiluaTheme())
    }

    @Test
    fun fromKiluaTheme_mapsAllValues() {
        assertEquals(ThemeMode.Light, ThemeMode.fromKiluaTheme(Theme.Light))
        assertEquals(ThemeMode.Dark, ThemeMode.fromKiluaTheme(Theme.Dark))
        assertEquals(ThemeMode.Auto, ThemeMode.fromKiluaTheme(Theme.Auto))
    }

    @Test
    fun roundTrip_isIdentity() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromKiluaTheme(mode.toKiluaTheme()))
        }
    }

    @Test
    fun themeState_defaultMode_isAuto() {
        val state = ThemeState()
        assertEquals(ThemeMode.Auto, state.mode)
    }

    @Test
    fun themeState_defaultIsDark_isFalse() {
        val state = ThemeState()
        assertFalse(state.isDark)
    }

    @Test
    fun themeState_equality() {
        assertEquals(ThemeState(ThemeMode.Dark, true), ThemeState(ThemeMode.Dark, isDark = true))
    }
}
