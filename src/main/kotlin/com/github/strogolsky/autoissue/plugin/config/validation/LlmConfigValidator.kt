package com.github.strogolsky.autoissue.plugin.config.validation

import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService

/**
 * Validates LLM configuration.
 *
 * Checks that LLM API key is present.
 */
class LlmConfigValidator(private val llmService: LlmAgentConfigService) : ConfigValidator {
    override val name = "LLM"
    override val configurableId = "com.github.strogolsky.autoissue.LLM"

    override fun isReady(): Boolean = llmService.isReady()

    override fun getErrorMessage(): String = "LLM API key is missing. Please configure LLM settings."
}
