package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.core.masking.MaskingConfig

data class PluginConfig(
    val llm: LlmDefaults,
    val renderingFormat: RenderingFormat,
    val enabledProviders: List<String>,
    val dev: DevConfig,
    val masking: MaskingConfig = MaskingConfig(),
)
