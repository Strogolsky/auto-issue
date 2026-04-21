package com.github.strogolsky.autoissue.startup

import com.github.strogolsky.autoissue.agent.GoogleLlmProviderFactory
import com.github.strogolsky.autoissue.agent.ModelProviderResolver
import com.github.strogolsky.autoissue.agent.context.ContextRegistry
import com.github.strogolsky.autoissue.agent.context.RendererFactory
import com.github.strogolsky.autoissue.agent.context.RendererFactoryHolder
import com.github.strogolsky.autoissue.agent.context.SimpleRendererFactory
import com.github.strogolsky.autoissue.agent.context.providers.ContextComponentProvider
import com.github.strogolsky.autoissue.agent.context.providers.FileContextComponentProvider
import com.github.strogolsky.autoissue.agent.context.providers.JiraMetadataProvider
import com.github.strogolsky.autoissue.config.LlmDefaults
import com.github.strogolsky.autoissue.config.LocalPropertiesLoader
import com.github.strogolsky.autoissue.config.PluginConfig
import com.github.strogolsky.autoissue.config.PluginConfigLoader
import com.github.strogolsky.autoissue.config.RenderingFormat
import com.github.strogolsky.autoissue.services.JiraConfigService
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PluginStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val config = PluginConfigLoader.load()

        if (config.dev.localPropertiesEnabled) {
            applyLocalProperties(project, config.dev.localPropertiesFile)
        }

        initModelProviders(project, config)
        initContextProviders(project, config)
        initRendering(project, config)
        applyLlmDefaults(project, config.llm)
        checkConfiguration(project)
    }

    private fun applyLocalProperties(
        project: Project,
        fileName: String,
    ) {
        val props = LocalPropertiesLoader.load(project, fileName)
        if (props.isEmpty()) return

        val agentConfig = project.service<AgentConfigService>()
        val jiraConfig = project.service<JiraConfigService>()

        props["autoissue.llm.api-key"]?.let {
            if (agentConfig.getApiKey().isNullOrBlank()) agentConfig.saveApiKey(it)
        }
        props["autoissue.jira.api-token"]?.let {
            if (jiraConfig.getApiToken().isNullOrBlank()) jiraConfig.saveApiToken(it)
        }
        props["autoissue.jira.base-url"]?.let {
            if (jiraConfig.state.baseUrl.isBlank()) jiraConfig.state.baseUrl = it
        }
        props["autoissue.jira.username"]?.let {
            if (jiraConfig.state.username.isBlank()) jiraConfig.state.username = it
        }
        props["autoissue.jira.project-key"]?.let {
            if (jiraConfig.state.defaultProjectKey.isBlank()) jiraConfig.state.defaultProjectKey = it
        }
    }

    private fun initModelProviders(
        project: Project,
        config: PluginConfig,
    ) {
        val resolver = project.service<ModelProviderResolver>()
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
        val factory: RendererFactory =
            when (config.renderingFormat) {
                RenderingFormat.SIMPLE -> SimpleRendererFactory()
            }
        project.service<RendererFactoryHolder>().factory = factory
    }

    private fun applyLlmDefaults(
        project: Project,
        defaults: LlmDefaults,
    ) {
        project.service<AgentConfigService>().applyDefaults(defaults)
    }

    private fun checkConfiguration(project: Project) {
        val agentConfig = project.service<AgentConfigService>()
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
