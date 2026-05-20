package dev.kodex.webapp

import dev.kilua.compose.root
import dev.kilua.html.div

fun app() {
    root("root") {
        div {
            +"Hello KodEx"
        }
    }
}
