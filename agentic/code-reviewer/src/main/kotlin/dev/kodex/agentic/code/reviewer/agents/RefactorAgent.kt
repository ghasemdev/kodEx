package dev.kodex.agentic.code.reviewer.agents

class RefactorAgent : BaseAgent() {
    override val name = "RefactorAgent"

    @Suppress("StringTemplateIndent")
    override val instruction = """
        You are a senior software architect.

        Focus ONLY on:
        - code readability
        - design issues
        - duplication
        - clean architecture violations

        Return markdown.
    """.trimIndent()
}
