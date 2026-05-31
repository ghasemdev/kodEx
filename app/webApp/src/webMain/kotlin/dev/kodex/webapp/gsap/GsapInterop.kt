@file:Suppress("PropertyName", "FunctionMinLength", "VariableMinLength", "unused")

package dev.kodex.webapp.gsap

import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.JsName
import kotlin.js.definedExternally
import org.w3c.dom.Element

@JsModule("gsap")
external val gsap: Gsap

@Suppress("ComplexInterface")
external interface Gsap {
    fun to(targets: Element, vars: GsapVars): JsAny
    fun from(targets: Element, vars: GsapVars): JsAny
    fun fromTo(targets: Element, fromVars: GsapVars, toVars: GsapVars): JsAny
    fun timeline(vars: GsapVars = definedExternally): JsAny
    fun set(targets: Element, vars: GsapVars)
    fun registerPlugin(vararg plugins: JsAny)
    fun matchMedia(): JsAny

    // Overloads accepting CSS selector strings, NodeList, or Array (JsAny covers all).
    @JsName("to")
    fun toMany(targets: JsAny, vars: GsapVars): JsAny

    @JsName("from")
    fun fromMany(targets: JsAny, vars: GsapVars): JsAny
}

// Build GSAP vars with unsafeJso<GsapVars> { ... } at callsites.
// All numeric properties use Double; only repeat uses Int (-1 = infinite).
@Suppress("ComplexInterface")
external interface GsapVars : JsAny {
    var x: Double
    var y: Double
    var opacity: Double
    var scale: Double
    var scaleX: Double
    var scaleY: Double
    var duration: Double
    var delay: Double
    var stagger: Double
    var repeat: Int
    var yoyo: Boolean
    var ease: String
    var transformOrigin: String
    var width: String
    var scrollTrigger: JsAny
    var onComplete: () -> Unit
    var onUpdate: () -> Unit
}

@JsModule("gsap/ScrollTrigger")
external val ScrollTrigger: JsAny

external interface ScrollTriggerConfig : JsAny {
    var trigger: Element
    var start: String
    var once: Boolean
}
