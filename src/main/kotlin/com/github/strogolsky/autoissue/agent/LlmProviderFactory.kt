package com.github.strogolsky.autoissue.agent

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

interface LlmProviderFactory {
    fun create(
        modelName: String,
        apiKey: String,
    ): Pair<PromptExecutor, LLModel>

    fun availableModels(): List<String>
}
