package dev.kodex.webapp.interop

import kotlin.js.JsAny

actual fun jsConstruct(ctor: JsAny, options: JsAny): JsAny =
    js("Reflect.construct(ctor, [options])").unsafeCast<JsAny>()
