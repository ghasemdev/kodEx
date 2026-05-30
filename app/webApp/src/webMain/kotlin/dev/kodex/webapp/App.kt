package dev.kodex.webapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.Application
import dev.kilua.CoreModule
import dev.kilua.Hot
import dev.kilua.TailwindcssModule
import dev.kilua.compose.root
import dev.kilua.i18n.LocaleManager
import dev.kilua.startApplication
import dev.kilua.theme.Theme
import dev.kilua.theme.ThemeManager
import dev.kilua.utils.isDom
import dev.kodex.webapp.design.i18n.initI18n
import dev.kodex.webapp.di.KoinApp
import dev.kodex.webapp.gsap.ScrollTrigger
import dev.kodex.webapp.gsap.gsap
import dev.kodex.webapp.playground.PlaygroundApp
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.plugin.module.dsl.startKoin

// import dev.kodex.webapp.pages.NotFoundPage
// import dev.kodex.webapp.pages.landing.LandingPage

@Suppress("LabeledExpression")
class App : Application() {
    override fun start() {
        startKoin<KoinApp> {
            printLogger()
        }

        gsap.registerPlugin(ScrollTrigger)

        ThemeManager.init(initialTheme = Theme.Auto, remember = true)

        MainScope().launch { initI18n() }

        if (isDom) {
            LocaleManager.setCurrentLocale(LocaleManager.currentLocale)
            LocaleManager.registerLocaleListener { locale ->
                val dir = if (locale.language.startsWith("fa")) "rtl" else "ltr"
                document.documentElement?.setAttribute("dir", dir)
            }
        }

        root("root") {
            val isDev = isDev()

            if (isDev && window.location.pathname.startsWith("/playground")) {
                PlaygroundApp()
                return@root
            }

            var currentPath by remember { mutableStateOf(window.location.pathname) }

            if (isDom) {
                window.addEventListener("popstate") {
                    currentPath = window.location.pathname
                }
            }

            /*when {
                currentPath == "/" || currentPath.isEmpty() -> LandingPage()
                else -> NotFoundPage()
            }*/
        }
    }
}

fun app() {
    startApplication(
        ::App,
        bundlerHot(),
        TailwindcssModule,
        CoreModule,
    )
}

expect fun bundlerHot(): Hot?
expect fun isDev(): Boolean
