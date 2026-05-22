package dev.kodex.webapp

import dev.kilua.Application
import dev.kilua.Hot
import dev.kilua.compose.root
import dev.kilua.html.div
import dev.kilua.startApplication

class App : Application() {
    override fun start() {
        root("root") {
            div {
                +"Hello KodEx"
            }
        }
    }
}

fun app() {
    startApplication(::App, bundlerHot())
}

expect fun bundlerHot(): Hot?
