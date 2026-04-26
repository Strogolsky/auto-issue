package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.core.agent.GoogleLlmProviderFactory
import com.github.strogolsky.autoissue.core.agent.LlmProviderRegistry
import com.github.strogolsky.autoissue.core.context.ContextRegistry
import com.github.strogolsky.autoissue.core.context.providers.ContextComponentProvider
import com.github.strogolsky.autoissue.core.context.providers.FileContextComponentProvider
import com.github.strogolsky.autoissue.core.context.providers.JiraMetadataProvider
import com.github.strogolsky.autoissue.core.context.render.PlainTextPromptRenderer
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.context.render.PromptRenderer
import com.github.strogolsky.autoissue.core.masking.ContentMasker
import com.github.strogolsky.autoissue.core.masking.MaskingPatterns
import com.github.strogolsky.autoissue.core.masking.RegexContentMasker
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmDefaults
import com.github.strogolsky.autoissue.plugin.config.PluginConfig
import com.github.strogolsky.autoissue.plugin.config.RenderingFormat
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val config = PluginConfigLoader.load()

        if (config.dev.localPropertiesEnabled) {
            applyLocalProperties(project)
        }

        initModelProviders(project, config)
        initContextProviders(project, config)
        initRendering(project, config)
        applyLlmDefaults(project, config.llm)
        checkConfiguration(project)
    }

    private fun applyLocalProperties(project: Project) {
        val agentConfig = project.service<LlmAgentConfigService>()
        val jiraConfig = project.service<JiraConfigService>()

        System.getProperty("autoissue.llm.api-key")?.takeIf { it.isNotBlank() }?.let { agentConfig.saveApiKey(it) }
        System.getProperty("autoissue.jira.api-token")?.takeIf { it.isNotBlank() }?.let { jiraConfig.saveApiToken(it) }
        System.getProperty("autoissue.jira.base-url")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.baseUrl = it }
        System.getProperty("autoissue.jira.username")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.username = it }
        System.getProperty("autoissue.jira.project-key")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.defaultProjectKey = it }
    }

    private fun initModelProviders(
        project: Project,
        config: PluginConfig,
    ) {
        val resolver = project.service<LlmProviderRegistry>()
        resolver.register("GOOGLE", GoogleLlmProviderFactory())
    }

    private fun initContextProviders(
        project: Project,
        config: PluginConfig,
    ) {
        val registry = project.service<ContextRegistry>()

        val available: Map<String, ContextComponentProvider> =
            mapOf(
                "FileContextComponentProvider" to FileContextComponentProvider(),
                "JiraMetadataProvider" to JiraMetadataProvider(project),
            )

        config.enabledProviders.forEach { name ->
            available[name]?.let { registry.register(it) }
                ?: logger.warn("AutoIssue: unknown context provider in config: $name")
        }
    }

    private fun initRendering(
        project: Project,
        config: PluginConfig,
    ) {
        val masker: ContentMasker =
            if (!config.masking.enabled) {
                ContentMasker { it }
            } else {
                RegexContentMasker(MaskingPatterns.ALL)
            }
        val factory: PromptRenderer =
            when (config.renderingFormat) {
                RenderingFormat.SIMPLE -> PlainTextPromptRenderer(masker)
            }
        project.service<PromptRenderService>().initialize(factory)
    }

    private fun applyLlmDefaults(
        project: Project,
        defaults: LlmDefaults,
    ) {
        project.service<LlmAgentConfigService>().applyDefaults(defaults)
    }

    private fun checkConfiguration(project: Project) {
        val agentConfig = project.service<LlmAgentConfigService>()
        val jiraConfig = project.service<JiraConfigService>()

        val apiKeyMissing = agentConfig.getApiKey().isNullOrBlank()
        val jiraUrlMissing = jiraConfig.state.baseUrl.isBlank()

        if (apiKeyMissing || jiraUrlMissing) {
            logger.warn(
                "AutoIssue: configuration incomplete — apiKey missing: $apiKeyMissing, jiraUrl missing: $jiraUrlMissing",
            )
        }
    }

    companion object {
        private val logger = Logger.getInstance(PluginStartupActivity::class.java)
    }
}
