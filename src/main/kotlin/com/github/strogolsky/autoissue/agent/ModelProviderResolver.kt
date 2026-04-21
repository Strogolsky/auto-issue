package com.github.strogolsky.autoissue.agent

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.PROJECT)
class ModelProviderResolver {
    private val factories = mutableMapOf<String, LlmProviderFactory>()

    fun register(
        providerKey: String,
        factory: LlmProviderFactory,
    ) {
        factories[providerKey] = factory
    }

    fun resolve(
        provider: String,
        modelName: String,
        apiKey: String,
    ): Pair<PromptExecutor, LLModel> {
        thisLogger().debug("Resolving model provider: $provider, model: $modelName")
        val factory =
            factories[provider.uppercase()]
                ?: error("Unknown LLM provider: $provider")
        return factory.create(modelName, apiKey)
    }
}
