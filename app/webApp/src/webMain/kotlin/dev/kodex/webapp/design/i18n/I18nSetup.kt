package dev.kodex.webapp.design.i18n

import dev.kilua.i18n.I18n
import dev.kilua.i18n.LocaleManager
import dev.kilua.i18n.SimpleLocale
import dev.kilua.utils.isDom
import kotlin.js.JsAny
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.await

var i18n: I18n = I18n()
    private set

/** Convert a Kotlin String to JsAny for passing into I18n constructor. */
expect fun String.asLocaleData(): JsAny

suspend fun initI18n() {
    if (!isDom) return
    val enContent = window.fetch("/modules/i18n/messages-en.po").await().text().await()
    val faContent = window.fetch("/modules/i18n/messages-fa.po").await().text().await()
    i18n = I18n(
        "en" to enContent.asLocaleData(),
        "fa" to faContent.asLocaleData(),
    )
    val savedLocale = localStorage.getItem("kodex-locale") ?: "en"
    LocaleManager.setCurrentLocale(SimpleLocale(language = savedLocale))
}

fun setLocale(code: String) {
    localStorage.setItem("kodex-locale", code)
    LocaleManager.setCurrentLocale(SimpleLocale(language = code))
}
