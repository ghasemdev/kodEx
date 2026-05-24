package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h3
import dev.kodex.webapp.design.components.CodeBlock

private val kotlinSample = """
fun fibonacci(n: Int): Int {
    if (n <= 1) return n
    var a = 0; var b = 1
    repeat(n - 1) { val tmp = a + b; a = b; b = tmp }
    return b
}

fun main() {
    (0..10).forEach { println(fibonacci(it)) }
}
""".trimIndent()

private val jsonSample = """
{
  "exam": "Kotlin Basics",
  "score": 92,
  "passed": true,
  "answers": [true, false, true, true, true]
}
""".trimIndent()

@Composable
fun IComponent.CodeBlockPreview() {
    div(className = "flex flex-col gap-6") {
        h3(className = "text-lg font-semibold text-on-surface") { +"Kotlin" }
        CodeBlock(code = kotlinSample, language = "kotlin")

        h3(className = "text-lg font-semibold text-on-surface") { +"JSON" }
        CodeBlock(code = jsonSample, language = "json")

        h3(className = "text-lg font-semibold text-on-surface") { +"No Copy Button" }
        CodeBlock(code = "val answer = 42", language = "kotlin", showCopyButton = false)
    }
}
