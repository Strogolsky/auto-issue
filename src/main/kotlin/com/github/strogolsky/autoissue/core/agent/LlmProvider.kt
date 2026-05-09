package com.github.strogolsky.autoissue.core.agent

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

/**
 * Interface for LLM provider implementations.
 *
 * Each provider (Google, Anthropic, OpenAI, etc.) implements this interface
 * to provide:
 * - A unique provider key for identification
 * - The default LLM model to use
 * - An executor that can make API calls with credentials
 *
 * Implementations are discovered via the extension point system.
 */
interface LlmProvider {
    /**
     * Unique identifier for this provider (e.g., "GOOGLE", "ANTHROPIC").
     *
     * Used to look up the provider in the registry.
     */
    val providerKey: String

    /**
     * The default LLM model to use for this provider.
     *
     * For example, "gemini-2.0-flash" for Google or "claude-3-5-sonnet" for Anthropic.
     */
    val defaultModel: LLModel

    /**
     * Creates an executor for making API calls to this provider.
     *
     * @param apiKey The API key/credentials for authentication
     * @return A PromptExecutor configured to call this provider's API
     */
    fun createExecutor(apiKey: String): PromptExecutor
}
