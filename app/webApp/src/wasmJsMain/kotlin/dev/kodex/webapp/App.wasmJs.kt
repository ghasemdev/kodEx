package dev.kodex.webapp

import dev.kilua.Hot

actual fun bundlerHot(): Hot? = js("import.meta.webpackHot")
