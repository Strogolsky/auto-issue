package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.plugin.config.validation.ConfigHealthChecker
import com.github.strogolsky.autoissue.ui.notifications.AutoIssueNotifier
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plugin startup activity executed when the IDE opens a project.
 *
 * Responsible for:
 * 1. Running initial setup (downloading models, initializing services, etc.)
 * 2. Checking configuration health and notifying users of missing settings
 * 3. Logging any configuration issues for debugging
 */
class PluginStartupActivity : ProjectActivity {
    /**
     * Executes on project load.
     *
     * @param project The project being loaded
     */
    override suspend fun execute(project: Project) {
        try {
            thisLogger().info("AutoIssue: Starting plugin initialization")
            withContext(Dispatchers.IO) {
                project.service<AutoIssueSetupTool>().setupEnvironmentIfNeeded()
            }
        } catch (e: Exception) {
            thisLogger().error("AutoIssue: setup failed during initialization", e)
            AutoIssueNotifier.notify(
                project,
                "Failed to initialize AutoIssue. Please check the logs.",
                NotificationType.ERROR,
            )
            return
        }

        // Check if configuration is complete
        if (!project.service<ConfigHealthChecker>().isSystemReady()) {
            thisLogger().warn("AutoIssue: configuration incomplete — LLM API key or JIRA credentials missing")
        } else {
            thisLogger().info("AutoIssue: plugin initialized successfully")
        }
    }
}
