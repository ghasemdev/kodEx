@file:Suppress("PropertyName", "FunctionMinLength", "VariableMinLength", "unused")

package dev.kodex.webapp.gsap

import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.definedExternally
import org.w3c.dom.Element

@JsModule("gsap")
external val gsap: Gsap

external interface Gsap {
    fun to(targets: Element, vars: GsapVars): JsAny
    fun from(targets: Element, vars: GsapVars): JsAny
    fun fromTo(targets: Element, fromVars: GsapVars, toVars: GsapVars): JsAny
    fun timeline(vars: GsapVars = definedExternally): JsAny
    fun set(targets: Element, vars: GsapVars)
    fun registerPlugin(vararg plugins: JsAny)
}

external interface GsapVars : JsAny {
    var x: Int
    var opacity: Int
    var duration: Int
    var ease: String
    var onComplete: () -> Unit
}

@JsModule("gsap/ScrollTrigger")
external val ScrollTrigger: JsAny
