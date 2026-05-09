package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.plugin.config.LangfuseConfig

/**
 * Loads Langfuse configuration from system properties.
 *
 * Langfuse is an optional LLM monitoring and observability service.
 * Configuration is loaded from system properties (typically set via:
 * - IDE run configuration VM options
 * - Maven/Gradle test properties
 * - Environment variables via property files
 *
 * System Properties:
 * - autoissue.langfuse.url: Langfuse API endpoint
 * - autoissue.langfuse.public-key: Public API key
 * - autoissue.langfuse.secret-key: Secret API key
 *
 * Returns null gracefully if any required property is missing, allowing
 * the plugin to function without Langfuse monitoring.
 */
object LangfuseConfigLoader {
    /**
     * Loads Langfuse configuration from system properties.
     *
     * All three properties (URL, public key, secret key) must be present and non-blank.
     *
     * @return LangfuseConfig if all required properties are present, or null otherwise
     */
    fun load(): LangfuseConfig? {
        val url = System.getProperty("autoissue.langfuse.url")?.takeIf { it.isNotBlank() } ?: return null
        val publicKey = System.getProperty("autoissue.langfuse.public-key")?.takeIf { it.isNotBlank() } ?: return null
        val secretKey = System.getProperty("autoissue.langfuse.secret-key")?.takeIf { it.isNotBlank() } ?: return null
        return LangfuseConfig(url, publicKey, secretKey)
    }
}
