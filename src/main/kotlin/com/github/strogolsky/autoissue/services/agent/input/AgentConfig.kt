package com.github.strogolsky.autoissue.services.agent.input

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

data class AgentConfig(
    val apiKey: String,
    val temperature: Double,
    val maxIterations: Int,
    val systemPrompt: String,
    val llmModel: LLModel,
    val promptExecutor: PromptExecutor,
)
