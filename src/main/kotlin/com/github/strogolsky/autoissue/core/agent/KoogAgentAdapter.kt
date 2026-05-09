package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import com.github.strogolsky.autoissue.core.exceptions.IssueGenerationException
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Adapter that wraps the Koog AI Agent to implement the IssueGenerationAgent interface.
 *
 * This adapter:
 * - Converts Koog AIAgent to our generic IssueGenerationAgent interface
 * - Adds logging for agent execution
 * - Converts Koog exceptions to AutoIssueException for consistent error handling
 *
 * @param I The input type
 * @param O The output type
 * @param agent The underlying Koog AI Agent
 */
class KoogAgentAdapter<I, O>(
    private val agent: AIAgent<I, O>,
) : IssueGenerationAgent<I, O> {
    /**
     * Runs the AI agent to generate an issue.
     *
     * Wraps the agent execution with logging and error handling.
     *
     * @param input The input containing instruction and context
     * @return The generated output (typically an issue candidate)
     * @throws IssueGenerationException if the agent fails
     */
    override suspend fun generate(input: I): O {
        thisLogger().info("Starting task generation process via Koog Agent")
        return try {
            val result = agent.run(input)
            thisLogger().info("Task generation completed successfully")
            result
        } catch (e: Exception) {
            thisLogger().warn("Task generation failed: ${e.message}", e)
            throw IssueGenerationException("Agent failed to generate issue: ${e.message}", e)
        }
    }
}
