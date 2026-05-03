package com.github.strogolsky.autoissue.plugin.config

data class LlmAgentConfig(
    val apiKey: String,
    val provider: String,
    val systemPrompt: String,
    val temperature: Double,
    val maxIterations: Int,
    val strategyId: String,
)
