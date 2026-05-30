package dev.kodex.agentic.code.reviewer

import dev.kodex.agentic.code.reviewer.agents.BugAgent
import dev.kodex.agentic.code.reviewer.agents.RefactorAgent
import dev.kodex.agentic.code.reviewer.core.ContextBuilder
import dev.kodex.agentic.code.reviewer.core.ReviewCoordinator
import dev.kodex.agentic.code.reviewer.tools.GitTool

suspend fun main() {
    val diff = GitTool.getDiff()

    val coordinator = ReviewCoordinator(
        agents = listOf(
            BugAgent(),
            RefactorAgent(),
        ),
        contextBuilder = ContextBuilder()
    )

    val result = coordinator.review(diff)

    println(result)
}
