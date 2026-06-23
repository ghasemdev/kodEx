package dev.kodex.webapp.interop

import kotlin.js.JsAny

// `new ctor(options)` where ctor is a named-export class obtained dynamically (e.g. via a
// namespace import). Needed because Kotlin/JS has no syntax to invoke `new` on a JsAny value.
expect fun jsConstruct(ctor: JsAny, options: JsAny): JsAny
