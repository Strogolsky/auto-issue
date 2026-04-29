package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.context.render.PromptRendererRegistry
import com.github.strogolsky.autoissue.core.masking.ContentMasker
import com.github.strogolsky.autoissue.core.masking.MaskingPatterns
import com.github.strogolsky.autoissue.core.masking.RegexContentMasker
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class AutoIssueSetupTool(private val project: Project) {
    fun setupEnvironmentIfNeeded() {
        val config = PluginConfigLoader.load()

        initRendering(config)

        val appProps = PropertiesComponent.getInstance()
        if (!appProps.getBoolean("AutoIssue_FirstRun")) {
            project.service<LlmAgentConfigService>().applyDefaults(config.llm)
            appProps.setValue("AutoIssue_FirstRun", true)
        }

        if (config.dev.localPropertiesEnabled) {
            applyLocalProperties()
        }
    }

    private fun initRendering(config: com.github.strogolsky.autoissue.plugin.config.PluginConfig) {
        val masker: ContentMasker =
            if (!config.masking.enabled) {
                ContentMasker { it }
            } else {
                RegexContentMasker(MaskingPatterns.ALL)
            }
        val renderer = project.service<PromptRendererRegistry>().resolve(config.renderingFormat)
        project.service<PromptRenderService>().initialize(renderer, masker)
    }

    private fun applyLocalProperties() {
        val agentConfig = project.service<LlmAgentConfigService>()
        val jiraConfig = project.service<JiraConfigService>()

        System.getProperty("autoissue.llm.api-key")?.takeIf { it.isNotBlank() }?.let { agentConfig.saveApiKey(it) }
        System.getProperty("autoissue.jira.api-token")?.takeIf { it.isNotBlank() }?.let { jiraConfig.saveApiToken(it) }
        System.getProperty("autoissue.jira.base-url")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.baseUrl = it }
        System.getProperty("autoissue.jira.username")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.username = it }
        System.getProperty("autoissue.jira.project-key")?.takeIf { it.isNotBlank() }?.let { jiraConfig.state.defaultProjectKey = it }
    }
}
