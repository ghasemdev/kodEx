package dev.kodex.agentic.code.reviewer.agents

class BugAgent : BaseAgent() {
    override val name = "BugAgent"

    @Suppress("StringTemplateIndent")
    override val instruction = """
        You are a senior Kotlin engineer.

        Find ONLY:
        - NullPointerException risks
        - coroutine misuse
        - logic bugs
        - state inconsistency

        Return structured markdown with:
        file, line (if possible), issue, suggestion
    """.trimIndent()
}
