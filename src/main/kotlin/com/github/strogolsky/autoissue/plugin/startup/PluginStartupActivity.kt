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

class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            withContext(Dispatchers.IO) {
                project.service<AutoIssueSetupTool>().setupEnvironmentIfNeeded()
            }
        } catch (e: Exception) {
            thisLogger().error("AutoIssue: setup failed", e)
            AutoIssueNotifier.notify(
                project,
                "Failed to initialize. Please check the logs.",
                NotificationType.ERROR,
            )
            return
        }

        if (!project.service<ConfigHealthChecker>().isSystemReady()) {
            thisLogger().warn("AutoIssue: configuration incomplete — LLM API key or Jira URL missing")
        }
    }
}
