package dev.kodex.webapp.gsap

import kotlinx.browser.window

fun prefersReducedMotion(): Boolean = runCatching {
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
}.getOrDefault(false)
