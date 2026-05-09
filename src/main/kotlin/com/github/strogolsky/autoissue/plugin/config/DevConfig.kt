package com.github.strogolsky.autoissue.plugin.config

/**
 * Development mode configuration.
 *
 * Controls whether to load plugin configuration from system properties,
 * useful for development and testing with temporary or test credentials.
 *
 * @param localPropertiesEnabled Whether to load config from system properties
 *                               (e.g., autoissue.jira.api-token, autoissue.llm.api-key)
 */
data class DevConfig(
    val localPropertiesEnabled: Boolean,
)
