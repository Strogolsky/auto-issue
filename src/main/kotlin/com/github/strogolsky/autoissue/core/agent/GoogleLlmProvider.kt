package com.github.strogolsky.autoissue.core.agent

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

/**
 * LLM provider for Google Gemini models.
 *
 * Provides access to Google's Gemini models through the Koog framework.
 * Uses Gemini 2.5 Flash Lite as the default model for cost efficiency.
 *
 * The provider creates a PromptExecutor that handles API calls to Google's LLM service.
 * API key must be a valid Google API key with Generative AI access.
 */
class GoogleLlmProvider : LlmProvider {
    override val providerKey = "GOOGLE"
    // Default to Gemini 2.5 Flash Lite for cost efficiency
    override val defaultModel: LLModel = GoogleModels.Gemini2_5FlashLite

    /**
     * Creates a PromptExecutor configured for Google Gemini API.
     *
     * @param apiKey The Google API key for authentication
     * @return A PromptExecutor for making API calls to Google's Gemini models
     */
    override fun createExecutor(apiKey: String): PromptExecutor = simpleGoogleAIExecutor(apiKey)
}
