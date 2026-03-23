package com.github.strogolsky.autoissue.settings

data class AgentConfig(
    val apiKey: String,
    val provider: String,
    val modelName: String,
    val systemPrompt: String,
    val temperature: Double,
    val maxIterations: Int,
    var strategyId: String,
)