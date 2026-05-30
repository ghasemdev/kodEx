package dev.kodex.agentic.code.reviewer.agents

class TestAgent : BaseAgent() {
    override val name = "TestAgent"
    override val instruction = """
You are a QA engineer.

Find missing tests:
- edge cases
- null cases
- failure paths
- integration gaps
"""
}
