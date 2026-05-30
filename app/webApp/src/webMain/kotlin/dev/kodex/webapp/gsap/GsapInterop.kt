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
    fun matchMedia(): JsAny
}

// Build GSAP vars with unsafeJso<GsapVars> { ... } at callsites.
// All numeric properties use Double; only repeat uses Int (-1 = infinite).
external interface GsapVars : JsAny {
    var x: Double
    var y: Double
    var opacity: Double
    var scale: Double
    var scaleX: Double
    var scaleY: Double
    var duration: Double
    var stagger: Double
    var repeat: Int
    var yoyo: Boolean
    var ease: String
    var transformOrigin: String
    var width: String
    var onComplete: () -> Unit
    var onUpdate: () -> Unit
}

@JsModule("gsap/ScrollTrigger")
external val ScrollTrigger: JsAny
