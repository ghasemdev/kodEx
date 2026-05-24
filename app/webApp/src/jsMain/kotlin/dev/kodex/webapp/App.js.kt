package dev.kodex.webapp

import dev.kilua.Hot

actual fun bundlerHot(): Hot? = js("import.meta.webpackHot").unsafeCast<Hot?>()
    ?: js("import.meta.hot").unsafeCast<Hot?>()

actual fun isDev(): Boolean= js("import.meta.env.DEV").unsafeCast<Boolean?>() ?: false
