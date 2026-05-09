package com.github.strogolsky.autoissue.plugin.config

/**
 * Configuration for Langfuse, an optional LLM monitoring and observability service.
 *
 * Langfuse tracks LLM API calls and provides analytics. Configuration is loaded
 * from system properties by LangfuseConfigLoader.
 *
 * System Properties:
 * - autoissue.langfuse.url: Langfuse API endpoint
 * - autoissue.langfuse.public-key: Public API key for authentication
 * - autoissue.langfuse.secret-key: Secret API key for authentication
 *
 * Optional: If not configured, the plugin works without Langfuse monitoring.
 *
 * @param url Langfuse API endpoint URL
 * @param publicKey Public API key for authentication
 * @param secretKey Secret API key for authentication
 */
data class LangfuseConfig(
    val url: String,
    val publicKey: String,
    val secretKey: String,
)
