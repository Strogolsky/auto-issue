package com.github.strogolsky.autoissue.services.agent.input

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

data class KoogAgentEnvironment(
    val executor: PromptExecutor,
    val model: LLModel,
    val toolRegistry: ToolRegistry,
    val systemPrompt: String,
    val temperature: Double,
    val maxIterations: Int,
)
