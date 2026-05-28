package dev.kodex.webapp.design

import dev.kilua.theme.Theme
import dev.kodex.webapp.design.theme.ThemeMode
import dev.kodex.webapp.design.theme.ThemeState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

class ThemeModeTest : FunSpec({
    context("toKiluaTheme") {
        test("Light maps to Theme.Light") { ThemeMode.Light.toKiluaTheme() shouldBe Theme.Light }
        test("Dark maps to Theme.Dark") { ThemeMode.Dark.toKiluaTheme() shouldBe Theme.Dark }
        test("Auto maps to Theme.Auto") { ThemeMode.Auto.toKiluaTheme() shouldBe Theme.Auto }
    }

    context("fromKiluaTheme") {
        test("Theme.Light maps to Light") { ThemeMode.fromKiluaTheme(Theme.Light) shouldBe ThemeMode.Light }
        test("Theme.Dark maps to Dark") { ThemeMode.fromKiluaTheme(Theme.Dark) shouldBe ThemeMode.Dark }
        test("Theme.Auto maps to Auto") { ThemeMode.fromKiluaTheme(Theme.Auto) shouldBe ThemeMode.Auto }
    }

    test("round-trip is identity for all values") {
        ThemeMode.entries.forEach { mode ->
            ThemeMode.fromKiluaTheme(mode.toKiluaTheme()) shouldBe mode
        }
    }

    context("ThemeState defaults") {
        test("default mode is Auto") { ThemeState().mode shouldBe ThemeMode.Auto }
        test("default isDark is false") { ThemeState().isDark.shouldBeFalse() }
        test("equality holds") { ThemeState(ThemeMode.Dark, true) shouldBe ThemeState(ThemeMode.Dark, isDark = true) }
    }
})
