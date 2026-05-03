package com.github.strogolsky.autoissue.core.agent

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

interface LlmProvider {
    val providerKey: String
    val defaultModel: LLModel

    fun createExecutor(apiKey: String): PromptExecutor
}
