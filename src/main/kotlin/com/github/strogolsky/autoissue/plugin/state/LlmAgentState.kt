package com.github.strogolsky.autoissue.plugin.state

/**
 * Persistent LLM agent configuration state.
 *
 * This class is persisted to disk by the IDE's state persistence system.
 * It stores non-sensitive LLM settings (the API key is stored separately
 * in the password safe).
 *
 * Default values (empty strings, 0.0, 0) indicate that configuration
 * has not been set, and defaults should be applied.
 *
 * @property provider The LLM provider name (GOOGLE, ANTHROPIC, etc.)
 * @property systemPrompt The system prompt for the agent
 * @property temperature Model temperature (0.0-1.0)
 * @property maxIterations Maximum agentic loop iterations
 * @property strategyId The generation strategy to use
 */
data class LlmAgentState(
    var provider: String = "",
    var systemPrompt: String = "",
    var temperature: Double = 0.0,
    var maxIterations: Int = 0,
    var strategyId: String = "",
)
