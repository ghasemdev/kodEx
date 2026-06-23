package dev.kodex.webapp.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kilua.utils.isDom
import kotlinx.browser.window

object Router {
    var currentPath: String by mutableStateOf(if (isDom) window.location.pathname else "/")
        private set

    fun navigate(path: String) {
        if (isDom) window.history.pushState(null, "", path)
        currentPath = path
    }

    fun syncFromBrowser() {
        if (isDom) currentPath = window.location.pathname
    }
}
