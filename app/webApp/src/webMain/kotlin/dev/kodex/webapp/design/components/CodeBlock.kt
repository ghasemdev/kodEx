package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.code
import dev.kilua.html.div
import dev.kilua.html.pre
import dev.kilua.html.span
import dev.kilua.utils.isDom
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import web.clipboard.writeText

@Composable
fun IComponent.CodeBlock(
    code: String,
    language: String = "plaintext",
    showCopyButton: Boolean = true,
    className: String? = null,
) {
    var copied by remember { mutableStateOf(false) }

    div(className = "relative rounded-xl overflow-hidden border border-outline/20 ${className ?: ""}".trim()) {
        if (showCopyButton) {
            div(className = "absolute top-2 end-2 z-10") {
                span(
                    className = "cursor-pointer inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs " +
                            "bg-surface-container/80 backdrop-blur-sm border border-outline/20 " +
                            "hover:bg-primary/10 text-on-surface transition-colors duration-150"
                ) {
                    +(if (copied) "Copied!" else "Copy")
                    onClick {
                        if (isDom) {
                            MainScope().launch {
                                web.navigator.navigator.clipboard.writeText(code)
                                copied = true
                                delay(2.seconds)
                                copied = false
                            }
                        }
                    }
                }
            }
        }
        pre(className = "overflow-x-auto p-4 m-0 text-sm font-mono bg-surface text-on-surface") {
            code(className = "language-$language") {
                +code
            }
        }
    }
}
