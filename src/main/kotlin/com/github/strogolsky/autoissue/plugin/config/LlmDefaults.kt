package com.github.strogolsky.autoissue.plugin.config

data class LlmDefaults(
    val provider: String,
    val modelName: String,
    val strategyId: String,
    val temperature: Double,
    val maxIterations: Int,
    val systemPrompt: String,
)
