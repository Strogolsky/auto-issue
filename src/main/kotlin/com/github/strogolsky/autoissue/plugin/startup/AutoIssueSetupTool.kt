package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.context.render.PromptRendererRegistry
import com.github.strogolsky.autoissue.core.masking.ContentMasker
import com.github.strogolsky.autoissue.core.masking.MaskingPatterns
import com.github.strogolsky.autoissue.core.masking.RegexContentMasker
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.plugin.config.PluginConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Initializes the AutoIssue plugin environment for a project.
 *
 * Handles:
 * - Loading plugin configuration from files
 * - Initializing the prompt rendering system (format + content masking)
 * - Applying default LLM configuration
 * - Setting up local development properties if enabled
 *
 * Called during plugin startup via PluginStartupActivity.
 *
 * Project-level service (separate instance per open project).
 */
@Service(Service.Level.PROJECT)
class AutoIssueSetupTool(private val project: Project) {
    /**
     * Performs one-time setup of the plugin environment.
     * Called once per project when the plugin first runs.
     *
     * Steps:
     * 1. Load plugin configuration from files
     * 2. Initialize prompt rendering with configured format and masking
     * 3. Apply default LLM configuration values
     * 4. If dev mode is enabled, load local development properties from system properties
     */
    fun setupEnvironmentIfNeeded() {
        val config = PluginConfigLoader.load()

        initRendering(config)

        ApplicationManager.getApplication().service<LlmAgentConfigService>().applyDefaults(config.llm)

        if (config.dev.localPropertiesEnabled) {
            applyLocalProperties()
        }
    }

    /**
     * Initializes the prompt rendering system.
     *
     * Sets up:
     * - The PromptRenderer based on configured rendering format (XML, Markdown, Simple)
     * - The ContentMasker (either enabled with regex patterns or disabled for debugging)
     *
     * Injects these into PromptRenderService for use throughout the plugin.
     *
     * @param config The loaded plugin configuration
     */
    private fun initRendering(config: PluginConfig) {
        val masker: ContentMasker =
            if (!config.masking.enabled) {
                ContentMasker { it }
            } else {
                RegexContentMasker(MaskingPatterns.ALL)
            }
        val renderer = project.service<PromptRendererRegistry>().resolve(config.renderingFormat)
        project.service<PromptRenderService>().initialize(renderer, masker)
    }

    /**
     * Applies configuration values from Java system properties.
     *
     * Useful for development and CI/CD where properties can be set via:
     * - IDE run configurations (VM options)
     * - Maven/Gradle test properties
     * - Environment variables (via property files)
     *
     * System properties override IDE settings:
     * - autoissue.llm.api-key
     * - autoissue.jira.api-token
     * - autoissue.jira.base-url
     * - autoissue.jira.username
     * - autoissue.jira.project-key
     */
    private fun applyLocalProperties() {
        val agentConfig = ApplicationManager.getApplication().service<LlmAgentConfigService>()
        val jiraConfig = ApplicationManager.getApplication().service<JiraConfigService>()

        System.getProperty("autoissue.llm.api-key")?.takeIf { it.isNotBlank() }?.let { agentConfig.saveApiKey(it) }
        System.getProperty("autoissue.jira.api-token")?.takeIf { it.isNotBlank() }?.let { jiraConfig.saveApiToken(it) }
        System.getProperty("autoissue.jira.base-url")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.baseUrl = it }
        System.getProperty("autoissue.jira.username")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.username = it }
        System.getProperty("autoissue.jira.project-key")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.defaultProjectKey = it }
    }
}
