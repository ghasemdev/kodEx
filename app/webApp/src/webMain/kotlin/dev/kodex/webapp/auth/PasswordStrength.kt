package dev.kodex.webapp.auth

import dev.kodex.webapp.interop.jsConstruct
import js.objects.unsafeJso
import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.unsafeCast

const val PASSWORD_STRENGTH_MIN_SCORE = 2

// @zxcvbn-ts/core only has named exports (no default), so it must be imported as a namespace
// object; ZxcvbnFactory is then instantiated via Reflect.construct (see jsConstruct).
@JsModule("@zxcvbn-ts/core")
private external object ZxcvbnCoreModule : JsAny {
    val ZxcvbnFactory: JsAny
}

private external interface ZxcvbnOptions : JsAny {
    var translations: JsAny
    var graphs: JsAny
    var dictionary: JsAny
}

private external interface ZxcvbnCheckResult : JsAny {
    val score: Int
}

private external interface ZxcvbnFactoryInstance : JsAny {
    fun check(password: String): ZxcvbnCheckResult
}

@JsModule("@zxcvbn-ts/language-common")
private external object ZxcvbnLanguageCommon : JsAny {
    val adjacencyGraphs: JsAny
    val dictionary: JsAny
}

@JsModule("@zxcvbn-ts/language-en")
private external object ZxcvbnLanguageEn : JsAny {
    val dictionary: JsAny
    val translations: JsAny
}

private val zxcvbn by lazy {
    jsConstruct(
        ZxcvbnCoreModule.ZxcvbnFactory,
        unsafeJso<ZxcvbnOptions> {
            translations = ZxcvbnLanguageEn.translations
            graphs = ZxcvbnLanguageCommon.adjacencyGraphs
            dictionary = ZxcvbnLanguageEn.dictionary
        },
    ).unsafeCast<ZxcvbnFactoryInstance>()
}

object PasswordStrength {
    fun score(password: String): Int {
        if (password.isBlank()) return 0
        return zxcvbn.check(password).score
    }

    fun label(score: Int): String = when (score) {
        0 -> "Very weak"
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Strong"
        else -> "Very strong"
    }
}
