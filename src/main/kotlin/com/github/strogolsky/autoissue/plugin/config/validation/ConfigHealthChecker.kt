package com.github.strogolsky.autoissue.plugin.config.validation

import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.ui.notifications.AutoIssueNotifier
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface ConfigValidator {
    val name: String
    val configurableId: String
    fun isReady(): Boolean
    fun getErrorMessage(): String
}

class JiraConfigValidator(private val jiraService: JiraConfigService) : ConfigValidator {
    override val name = "Jira"
    override val configurableId = "com.github.strogolsky.autoissue.Jira"
    override fun isReady(): Boolean = jiraService.isReady()
    override fun getErrorMessage(): String = "Jira Base URL or credentials are missing."
}

class LlmConfigValidator(private val llmService: LlmAgentConfigService) : ConfigValidator {
    override val name = "LLM"
    override val configurableId = "com.github.strogolsky.autoissue.LLM"
    override fun isReady(): Boolean = llmService.isReady()
    override fun getErrorMessage(): String = "LLM API key is missing."
}

@Service(Service.Level.PROJECT)
class ConfigHealthChecker(private val project: Project) {

    private val validators: List<ConfigValidator> by lazy {
        listOf(
            JiraConfigValidator(project.service<JiraConfigService>()),
            LlmConfigValidator(project.service<LlmAgentConfigService>()),
        )
    }

    fun isSystemReady(): Boolean = validators.all { it.isReady() }

    fun validateAndNotify(): Boolean {
        val failed = validators.filter { !it.isReady() }

        if (failed.isEmpty()) return true

        if (failed.size > 1) {
            AutoIssueNotifier.notifyMissingConfig(
                project,
                "Multiple configurations are missing. Please configure the plugin.",
                "com.github.strogolsky.autoissue.Main",
            )
        } else {
            val validator = failed.first()
            AutoIssueNotifier.notifyMissingConfig(
                project,
                validator.getErrorMessage(),
                validator.configurableId,
            )
        }
        return false
    }
}
