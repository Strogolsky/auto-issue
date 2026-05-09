package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.core.masking.MaskingConfig

/**
 * Complete plugin configuration loaded from PluginConfig.xml.
 *
 * Contains all configuration necessary to initialize the AutoIssue plugin:
 * - LLM defaults (provider, strategy, temperature, system prompt)
 * - Prompt rendering format (XML, Markdown, Simple)
 * - Development mode settings
 * - Content masking settings
 *
 * Loaded once at startup by PluginConfigLoader and used by AutoIssueSetupTool
 * to initialize the plugin environment.
 *
 * @param llm LLM defaults (provider, strategy, temperature, max iterations, system prompt)
 * @param renderingFormat The prompt format to use ("XML", "MARKDOWN", or "SIMPLE")
 * @param dev Development mode configuration
 * @param masking Content masking settings (default: enabled)
 */
data class PluginConfig(
    val llm: LlmDefaults,
    val renderingFormat: String,
    val dev: DevConfig,
    val masking: MaskingConfig = MaskingConfig(),
)
