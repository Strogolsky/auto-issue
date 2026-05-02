package com.github.strogolsky.autoissue.core.agent

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

class GoogleLlmProvider : LlmProvider {
    override val providerKey = "GOOGLE"
    override val defaultModel: LLModel = GoogleModels.Gemini2_5FlashLite

    override fun createExecutor(apiKey: String): PromptExecutor = simpleGoogleAIExecutor(apiKey)
}
