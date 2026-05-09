package com.github.strogolsky.autoissue.plugin.config

/**
 * LLM agent configuration for AI-driven issue generation.
 *
 * Contains settings for the AI model, generation strategy, and behavioral parameters.
 * This is the effective config passed to the agent factory for creation.
 *
 * @param apiKey The API key for the LLM provider
 * @param provider The LLM provider name (e.g., "GOOGLE", "ANTHROPIC")
 * @param systemPrompt The system prompt/instructions for the AI agent
 * @param temperature The model temperature (0.0-1.0) controlling randomness
 *                    Lower values (e.g., 0.3) for deterministic generation
 *                    Higher values (e.g., 0.8) for more creative output
 * @param maxIterations Maximum number of iterations for the agentic loop
 * @param strategyId The generation strategy to use (e.g., "DIRECT", "REASONING")
 */
data class LlmAgentConfig(
    val apiKey: String,
    val provider: String,
    val systemPrompt: String,
    val temperature: Double,
    val maxIterations: Int,
    val strategyId: String,
)
