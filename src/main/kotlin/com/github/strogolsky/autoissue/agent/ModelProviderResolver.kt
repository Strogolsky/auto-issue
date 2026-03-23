package com.github.strogolsky.autoissue.agent

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class ModelProviderResolver {


    fun resolve(provider: String, modelName: String, apiKey: String): Pair<PromptExecutor, LLModel> {
        return when (provider.uppercase()) {
            "GOOGLE" -> {
                val executor = simpleGoogleAIExecutor(apiKey)
                val model = when (modelName.lowercase()) {
                    "gemini-2.5-flash" -> GoogleModels.Gemini2_5Flash
                    "gemini-2.5-pro" -> GoogleModels.Gemini2_5Pro
                    else -> throw IllegalArgumentException("Unsupported Google model: $modelName")
                }
                Pair(executor, model)
            }
            // "OPENAI" -> ...
            // "OLLAMA" -> ...
            else -> throw IllegalArgumentException("Unsupported AI provider: $provider")
        }
    }
}