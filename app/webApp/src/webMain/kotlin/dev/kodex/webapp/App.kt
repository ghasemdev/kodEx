package dev.kodex.webapp

import dev.kilua.Application
import dev.kilua.CoreModule
import dev.kilua.FontAwesomeModule
import dev.kilua.Hot
import dev.kilua.TailwindcssModule
import dev.kilua.compose.root
import dev.kilua.html.div
import dev.kilua.i18n.LocaleManager
import dev.kilua.startApplication
import dev.kilua.theme.Theme
import dev.kilua.theme.ThemeManager
import dev.kilua.utils.isDom
import dev.kodex.webapp.design.i18n.initI18n
import dev.kodex.webapp.playground.PlaygroundApp
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class App : Application() {
    override fun start() {
        ThemeManager.init(initialTheme = Theme.Auto, remember = true)

        // Initialize i18n async — RTL dir applied when locale loads
        MainScope().launch {
            initI18n()
        }

        // RTL side-effect: update document dir on locale change
        if (isDom) {
            LocaleManager.setCurrentLocale(LocaleManager.currentLocale)
            LocaleManager.registerLocaleListener { locale ->
                val dir = if (locale.language.startsWith("fa")) "rtl" else "ltr"
                document.documentElement?.setAttribute("dir", dir)
            }
        }

        root("root") {
            @Suppress("UNCHECKED_CAST")
            val isDev = isDev()
            if (isDev && window.location.pathname.startsWith("/playground")) {
                PlaygroundApp()
            } else {
                div {
                    +"Hello KodEx"
                }
            }
        }
    }
}

fun app() {
    startApplication(
        ::App,
        bundlerHot(),
        TailwindcssModule,
        FontAwesomeModule,
        CoreModule,
    )
}

expect fun bundlerHot(): Hot?
expect fun isDev(): Boolean
