package com.github.strogolsky.autoissue.config

import com.github.strogolsky.autoissue.masking.MaskingConfig

data class PluginConfig(
    val llm: LlmDefaults,
    val renderingFormat: RenderingFormat,
    val enabledProviders: List<String>,
    val dev: DevConfig,
    val masking: MaskingConfig = MaskingConfig(),
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
