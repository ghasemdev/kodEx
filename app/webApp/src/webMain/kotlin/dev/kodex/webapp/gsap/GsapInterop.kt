package dev.kodex.webapp.gsap

import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.definedExternally

@JsModule("gsap")
@JsNonModule
external object Gsap {
    fun to(targets: JsAny, vars: JsAny): JsAny
    fun from(targets: JsAny, vars: JsAny): JsAny
    fun fromTo(targets: JsAny, fromVars: JsAny, toVars: JsAny): JsAny
    fun timeline(vars: JsAny = definedExternally): JsAny
    fun set(targets: JsAny, vars: JsAny)
    fun registerPlugin(vararg plugins: JsAny)
    fun matchMedia(): JsAny
    fun killTweensOf(targets: JsAny)
}

@JsModule("gsap/ScrollTrigger")
@JsNonModule
external val ScrollTrigger: JsAny
