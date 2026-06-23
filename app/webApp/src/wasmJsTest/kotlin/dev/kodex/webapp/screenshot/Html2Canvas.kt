@file:JsModule("html2canvas")

package dev.kodex.webapp.screenshot

import kotlin.js.Promise
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

@JsName("default")
external fun html2canvas(element: HTMLElement): Promise<HTMLCanvasElement>
