package dev.kodex.webapp

import dev.kilua.Hot

actual fun bundlerHot(): Hot? = js("import.meta.webpackHot").unsafeCast<Hot?>()
    ?: js("import.meta.hot").unsafeCast<Hot?>()
