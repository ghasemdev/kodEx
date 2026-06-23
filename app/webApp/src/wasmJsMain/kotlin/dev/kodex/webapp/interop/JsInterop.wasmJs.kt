package dev.kodex.webapp.interop

@JsFun("(ctor, options) => Reflect.construct(ctor, [options])")
private external fun jsConstructImpl(ctor: JsAny, options: JsAny): JsAny

actual fun jsConstruct(ctor: JsAny, options: JsAny): JsAny = jsConstructImpl(ctor, options)
