package com.github.strogolsky.autoissue.config

data class PluginConfig(
    val llm: LlmDefaults,
    val renderingFormat: RenderingFormat,
    val enabledProviders: List<String>,
    val dev: DevConfig,
)

data class LlmDefaults(
    val provider: String,
    val modelName: String,
    val strategyId: String,
    val temperature: Double,
    val maxIterations: Int,
    val systemPrompt: String,
)

data class DevConfig(
    val localPropertiesEnabled: Boolean,
)

enum class RenderingFormat { SIMPLE }
