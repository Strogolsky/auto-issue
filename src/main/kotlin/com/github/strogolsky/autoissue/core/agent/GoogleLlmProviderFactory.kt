package com.github.strogolsky.autoissue.core.agent

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

class GoogleLlmProviderFactory : LlmProviderFactory {
    override fun providerKey() = "GOOGLE"

    override fun create(
        modelName: String,
        apiKey: String,
    ): Pair<PromptExecutor, LLModel> {
        val executor = simpleGoogleAIExecutor(apiKey)
        val model =
            GoogleModels.models.find { it.id == modelName }
                ?: error("Unsupported Google model: $modelName")
        return executor to model
    }

    override fun availableModels(): List<String> = GoogleModels.models.map { it.id }
}
