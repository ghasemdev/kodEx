package dev.kodex.webapp

import dev.kilua.Hot

actual fun bundlerHot(): Hot? = null

// import.meta.env.DEV is set by Vite; fall back to process.env.NODE_ENV for webpack.
@JsFun(
    "() => import.meta.env?.DEV === true || " +
        "(typeof process !== 'undefined' && process.env?.NODE_ENV === 'development')",
)
private external fun checkDevMode(): Boolean

actual fun isDev(): Boolean = checkDevMode()
