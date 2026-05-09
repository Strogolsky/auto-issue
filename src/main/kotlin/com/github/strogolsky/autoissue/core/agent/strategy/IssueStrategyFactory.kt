package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import com.intellij.openapi.project.Project

/**
 * Factory for creating issue generation strategies.
 *
 * Different LLM providers may have different strategies for generating issues:
 * - DIRECT: Simple direct generation without intermediate steps
 * - REASONING: Multi-step reasoning approach with intermediate analysis
 *
 * Each strategy implementation is discovered via extension points and
 * registered for its corresponding LLM provider.
 *
 * @param I The input type for the strategy
 * @param O The output type for the strategy
 */
interface IssueStrategyFactory<I, O> {
    /**
     * The LLM provider this strategy is for (e.g., "GOOGLE", "ANTHROPIC").
     */
    val providerKey: String

    /**
     * Unique identifier for this strategy (e.g., "DIRECT", "REASONING").
     */
    val id: String

    /**
     * Human-readable display name for the strategy (shown in UI).
     */
    val displayName: String

    /**
     * Creates a strategy instance configured for the given project.
     *
     * @param project The IntelliJ project context
     * @return A configured AI agent graph strategy
     */
    fun createStrategy(project: Project): AIAgentGraphStrategy<I, O>
}
