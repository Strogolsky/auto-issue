package com.github.strogolsky.autoissue.agent

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

class GoogleLlmProviderFactory : LlmProviderFactory {
    override fun create(
        modelName: String,
        apiKey: String,
    ): Pair<PromptExecutor, LLModel> {
        val executor = simpleGoogleAIExecutor(apiKey)
        val model =
            when (modelName.lowercase()) {
                "gemini-2.5-flash" -> GoogleModels.Gemini2_5Flash
                "gemini-2.5-pro" -> GoogleModels.Gemini2_5Pro
                else -> throw IllegalArgumentException("Unsupported Google model: $modelName")
            }
        return Pair(executor, model)
    }
}
