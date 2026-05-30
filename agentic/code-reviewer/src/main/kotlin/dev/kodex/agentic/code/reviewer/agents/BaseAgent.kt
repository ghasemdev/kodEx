package dev.kodex.agentic.code.reviewer.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

abstract class BaseAgent {
    abstract val name: String
    abstract val instruction: String

    protected open val temperature: Double = 0.2
    protected open val maxIterations: Int = 10

    private val agent by lazy {
        AIAgent(
            promptExecutor = simpleGoogleAIExecutor(""),
            systemPrompt = instruction,
            llmModel = GoogleModels.Gemini2_5Flash,
            temperature = temperature,
            maxIterations = maxIterations
        )
    }

    suspend fun run(context: String): String {
        return agent.run(context)
    }
}
