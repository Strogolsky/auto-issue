package com.github.strogolsky.autoissue.plugin.config

/**
 * Default values for LLM agent configuration.
 *
 * Used during initial setup or when configuration values are missing.
 * These defaults are applied to empty configuration fields.
 *
 * @param provider Default LLM provider
 * @param strategyId Default generation strategy
 * @param temperature Default model temperature
 * @param maxIterations Default maximum agentic loop iterations
 * @param systemPrompt Default system prompt for the agent
 */
data class LlmDefaults(
    val provider: String,
    val strategyId: String,
    val temperature: Double,
    val maxIterations: Int,
    val systemPrompt: String,
)
