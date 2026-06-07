package dev.kodex.agentic.code.reviewer.agents

class AndroidAgent : BaseAgent() {
    override val name = "AndroidAgent"

    @Suppress("StringTemplateIndent")
    override val instruction = """
        You are a senior Android engineer.

        Focus on:
        - Compose state handling
        - lifecycle leaks
        - coroutine scope misuse
        - ViewModel design

        Ignore general Java issues.
    """.trimIndent()
}
