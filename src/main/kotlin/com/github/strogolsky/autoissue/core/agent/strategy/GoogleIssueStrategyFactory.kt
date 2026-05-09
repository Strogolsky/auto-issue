package com.github.strogolsky.autoissue.core.agent.strategy

/**
 * Base class for Google LLM provider strategies.
 *
 * All strategy factories for the Google provider (Gemini) inherit from this class.
 * This allows grouping strategies by provider and discovering them via extension points.
 *
 * Implementations:
 * - JiraDirectStrategyFactory: Single-step issue generation
 * - JiraReasoningStrategyFactory: Multi-step reasoning with analysis
 *
 * @param I The input type for the strategy
 * @param O The output type for the strategy
 */
abstract class GoogleIssueStrategyFactory<I, O> : IssueStrategyFactory<I, O> {
    override val providerKey = "GOOGLE"
}
