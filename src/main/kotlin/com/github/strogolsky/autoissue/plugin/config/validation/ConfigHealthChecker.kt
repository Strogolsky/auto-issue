package com.github.strogolsky.autoissue.plugin.config.validation

import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.ui.notifications.AutoIssueNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Checks the health of all plugin configurations.
 *
 * Validates all configuration subsystems (JIRA, LLM) and provides
 * notifications when configuration is incomplete.
 */
@Service(Service.Level.PROJECT)
class ConfigHealthChecker(private val project: Project) {
    private val validators: List<ConfigValidator> by lazy {
        listOf(
            JiraConfigValidator(ApplicationManager.getApplication().service<JiraConfigService>()),
            LlmConfigValidator(ApplicationManager.getApplication().service<LlmAgentConfigService>()),
        )
    }

    /**
     * Checks if all configurations are ready.
     *
     * @return true if all validators pass, false if any are missing
     */
    fun isSystemReady(): Boolean = validators.all { it.isReady() }

    /**
     * Validates configurations and notifies user of any missing settings.
     *
     * Shows appropriate error message based on which configurations are missing.
     *
     * @return true if all configurations are valid, false otherwise
     */
    fun validateAndNotify(): Boolean {
        val failed = validators.filter { !it.isReady() }

        if (failed.isEmpty()) return true

        if (failed.size > 1) {
            // Multiple configurations missing - show general message with main settings link
            AutoIssueNotifier.notifyMissingConfig(
                project,
                "Multiple configurations are missing. Please configure the plugin in settings.",
                "com.github.strogolsky.autoissue.Main",
            )
        } else {
            // Single configuration missing - show specific error and direct link
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
