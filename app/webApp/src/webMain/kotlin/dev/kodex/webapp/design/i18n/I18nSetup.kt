package dev.kodex.webapp.design.i18n

import androidx.compose.runtime.mutableStateOf
import dev.kilua.i18n.I18n
import dev.kilua.i18n.LocaleManager
import dev.kilua.i18n.SimpleLocale
import dev.kilua.utils.isDom
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.js.JsAny
import kotlinx.browser.localStorage

// Backed by MutableState so any composable that calls i18n.tr() is automatically
// subscribed: when initI18n() assigns a new I18n instance, Compose invalidates
// all callers directly — no dirty-counter reads needed per section.
@Suppress("PropertyName", "RedundantSuppression")
private val _i18nState = mutableStateOf(I18n())

var i18n: I18n
    get() = _i18nState.value
    private set(value) { _i18nState.value = value }

private val ALLOWED_LOCALES = setOf("en", "fa")

/** Convert a Kotlin String to JsAny for passing into I18n constructor. */
expect fun String.asLocaleData(): JsAny

suspend fun initI18n() {
    if (!isDom) return
    val client = HttpClient()
    val enContent = client.get("/modules/i18n/messages-en.po").bodyAsText()
    val faContent = client.get("/modules/i18n/messages-fa.po").bodyAsText()
    client.close()
    i18n = I18n(
        "en" to enContent.asLocaleData(),
        "fa" to faContent.asLocaleData(),
    )
    val savedLocale = localStorage.getItem("kodex-locale")
        ?.takeIf { it in ALLOWED_LOCALES }
        ?: "en"
    LocaleManager.setCurrentLocale(SimpleLocale(language = savedLocale))
}

fun setLocale(code: String) {
    require(code in ALLOWED_LOCALES) { "Unsupported locale: $code" }
    localStorage.setItem("kodex-locale", code)
    LocaleManager.setCurrentLocale(SimpleLocale(language = code))
}
