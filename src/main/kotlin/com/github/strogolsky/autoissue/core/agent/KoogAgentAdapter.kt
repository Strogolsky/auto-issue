package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import com.github.strogolsky.autoissue.core.exceptions.IssueGenerationException
import com.intellij.openapi.diagnostic.thisLogger

class KoogAgentAdapter<I, O>(
    private val agent: AIAgent<I, O>,
) : IssueGenerationAgent<I, O> {
    override suspend fun generate(input: I): O {
        thisLogger().info("Starting task generation process via Koog Agent.")
        return try {
            val result = agent.run(input)
            thisLogger().info("Generation process completed successfully.")
            result
        } catch (e: Exception) {
            thisLogger().warn("Task generation failed: ${e.message}", e)
            throw IssueGenerationException("Agent failed: ${e.message}", e)
        }
    }
}
