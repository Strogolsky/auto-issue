package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import com.intellij.openapi.diagnostic.thisLogger

class KoogIssueGenerationAgent<I, O>(
    private val agent: AIAgent<I, O>,
) : IssueGenerationAgent<I, O> {
    override suspend fun generate(input: I): O? {
        thisLogger().info("Starting task generation process via Koog Agent.")
        return try {
            val result = agent.run(input)
            thisLogger().info("Generation process completed successfully.")
            result
        } catch (e: Exception) {
            thisLogger().error("Critical failure during task generation: ${e.message}", e)
            null
        }
    }
}
